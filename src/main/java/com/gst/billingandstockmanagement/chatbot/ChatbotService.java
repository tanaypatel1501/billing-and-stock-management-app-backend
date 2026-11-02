package com.gst.billingandstockmanagement.chatbot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final int MAX_ATTEMPTS = 2;
    private static final int MAX_ROWS_TO_LLM = 50;
    private static final int MAX_TABLE_ROWS = 100;
    private static final int MAX_ROWS = 200;
    private static final int MAX_HISTORY_TURNS = 6;
    private static final int MAX_TURN_CHARS = 300;

    private final LlmClient llm;
    private final SqlGuard guard;
    private final ChatbotSqlExecutor executor;
    private final EntityResolver resolver;
    private final PendingChoiceStore pending;
    private final ObjectMapper mapper;

    // ------------------------------------------------------------------ new question

    public ChatbotResponse ask(Long userId, String question, List<ChatbotRequest.Turn> history) {
        Lang lang = LanguageDetector.detect(question, history);      // decided in code, from what the user typed

        String system = ChatbotPrompts.sqlSystemPrompt(LocalDate.now(IST));
        String base = historyBlock(history)
                + "Reply language: " + lang.instruction() + "\n"
                + "Current message: " + question;
        String lastSql = null;
        String lastError = null;

        try {
            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                String prompt = lastError == null ? base
                        : base + "\n\nYour previous reply was rejected.\nQuery: " + lastSql
                        + "\nReason: " + lastError + "\nFix it and return the JSON object again.";

                JsonNode plan;
                try {
                    plan = mapper.readTree(stripFences(llm.completeJson(system, prompt)));
                } catch (JsonProcessingException e) {
                    lastError = "Reply was not valid JSON.";
                    continue;
                }

                String intent = plan.path("intent").asText("");

                // Out of context: stop here. No SQL is validated or executed, whatever the model put in "sql".
                if ("out_of_scope".equals(intent)) {
                    return ChatbotResponse.text("refused", Msg.REFUSAL.in(lang));
                }
                if ("not_available".equals(intent)) {
                    String msg = plan.path("message").asText("");
                    return ChatbotResponse.text("not_available",
                            msg.isBlank() ? Msg.NOT_AVAILABLE_DEFAULT.in(lang) : msg);
                }
                if ("ambiguous".equals(intent)) {
                    String msg = plan.path("message").asText("");
                    return ChatbotResponse.text("clarification_needed",
                            msg.isBlank() ? Msg.CLARIFY_DEFAULT.in(lang) : msg);
                }
                if (!"answerable".equals(intent)) {
                    lastError = "intent must be answerable, not_available, out_of_scope or ambiguous.";
                    continue;
                }

                lastSql = plan.path("sql").asText("");
                String standalone = plan.path("standalone_question").asText("").trim();
                if (standalone.isEmpty() || standalone.length() > 400) standalone = question;

                JsonNode entity = plan.path("entity");
                String kind = entity.path("kind").asText("");
                String typed = entity.path("text").asText("").trim();
                boolean hasEntity = !typed.isEmpty();
                long placeholders = lastSql.chars().filter(c -> c == '?').count();

                if (hasEntity && !kind.equals("purchaser") && !kind.equals("product")) {
                    lastError = "entity.kind must be purchaser or product.";
                    continue;
                }
                if (placeholders > 1 || hasEntity != (placeholders == 1)) {
                    lastError = "Use exactly one ? placeholder if and only if a specific purchaser or product name is mentioned.";
                    continue;
                }

                try {
                    String safeSql = guard.validate(lastSql);

                    if (!hasEntity) {
                        return answered(standalone, safeSql, executor.run(userId, safeSql, List.of(), MAX_ROWS), lang);
                    }

                    NameMatcher.Result found = resolver.resolve(userId, kind, typed);
                    switch (found.outcome()) {
                        case NONE:
                            return ChatbotResponse.text("not_found",
                                    Msg.NOT_FOUND.in(lang, Msg.kind(kind, lang), typed));
                        case ONE:
                            return answered(standalone, safeSql,
                                    executor.run(userId, safeSql, List.of(found.names().get(0)), MAX_ROWS), lang);
                        default: {
                            String token = pending.put(userId, standalone, safeSql, found.names(), lang);
                            if (token == null) return busy(lang);
                            return new ChatbotResponse("needs_choice",
                                    Msg.CHOICE_PROMPT.in(lang, Msg.kind(kind, lang), typed),
                                    null, null, null, found.names(), token);
                        }
                    }
                } catch (SqlGuard.RejectedSqlException | ChatbotSqlExecutor.QueryFailedException e) {
                    lastError = e.getMessage();
                    log.info("Chatbot query rejected (attempt {}): {}", attempt, lastError);
                }
            }
            return ChatbotResponse.text("failed", Msg.FAILED.in(lang));

        } catch (LlmClient.LlmException e) {
            log.warn("Chatbot LLM failure: {}", e.getMessage());
            return busy(lang);
        }
    }

    // ------------------------------------------------------------------ user picked an option

    public ChatbotResponse answerChoice(Long userId, String token, String choice) {
        PendingChoiceStore.Pending p = pending.take(token, userId);
        if (p == null) {
            return ChatbotResponse.text("failed", Msg.EXPIRED_CHOICE.in(Lang.ENGLISH));
        }
        if (choice == null || !p.options().contains(choice)) {
            return ChatbotResponse.text("failed", Msg.INVALID_CHOICE.in(p.lang()));
        }
        try {
            return answered(p.question(), p.sql(), executor.run(userId, p.sql(), List.of(choice), MAX_ROWS), p.lang());
        } catch (ChatbotSqlExecutor.QueryFailedException e) {
            log.info("Chatbot choice query failed: {}", e.getMessage());
            return ChatbotResponse.text("failed", Msg.TRY_AGAIN.in(p.lang()));
        }
    }

    // ------------------------------------------------------------------ helpers

    private ChatbotResponse answered(String question, String sql, List<Map<String, Object>> rows, Lang lang) {
        log.info("Chatbot rows={} sql={}", rows.size(), sql);
        String answer = summarize(question, rows, lang);

        List<String> columns = null;
        List<List<Object>> table = null;
        if (rows.size() > 1) {
            columns = new ArrayList<>(rows.get(0).keySet());
            table = rows.stream().limit(MAX_TABLE_ROWS)
                    .map(r -> (List<Object>) new ArrayList<Object>(r.values()))
                    .toList();
        }
        return new ChatbotResponse("answered", answer, sql, columns, table, null, null);
    }

    private static ChatbotResponse busy(Lang lang) {
        return ChatbotResponse.text("failed", Msg.BUSY.in(lang));
    }

    /** Last few turns, trimmed. Only the text, never rows or SQL. */
    private static String historyBlock(List<ChatbotRequest.Turn> history) {
        if (history == null || history.isEmpty()) return "";
        List<ChatbotRequest.Turn> recent =
                history.subList(Math.max(0, history.size() - MAX_HISTORY_TURNS), history.size());

        StringBuilder sb = new StringBuilder("Conversation so far (oldest first):\n");
        for (ChatbotRequest.Turn t : recent) {
            if (t == null || t.text() == null || t.text().isBlank()) continue;
            String text = t.text().replaceAll("\\s+", " ").trim();
            if (text.length() > MAX_TURN_CHARS) text = text.substring(0, MAX_TURN_CHARS);
            sb.append("user".equals(t.role()) ? "User: " : "Assistant: ").append(text).append('\n');
        }
        return sb.append('\n').toString();
    }

    private String summarize(String question, List<Map<String, Object>> rows, Lang lang) {
        if (rows.isEmpty()) return Msg.NO_ROWS.in(lang);

        List<Map<String, Object>> sample = rows.size() > MAX_ROWS_TO_LLM ? rows.subList(0, MAX_ROWS_TO_LLM) : rows;
        try {
            String payload = "Reply language: " + lang.instruction()
                    + "\nQuestion: " + question
                    + "\nTotal rows: " + rows.size() + " (showing " + sample.size() + ")"
                    + "\nRows (JSON): " + mapper.writeValueAsString(sample);
            JsonNode res = mapper.readTree(stripFences(llm.completeJson(ChatbotPrompts.ANSWER_SYSTEM, payload)));
            String answer = res.path("answer").asText("");
            if (!answer.isBlank()) return answer;
        } catch (Exception e) {
            log.warn("Answer step failed, using plain fallback: {}", e.getClass().getSimpleName());
        }
        return fallbackText(rows, lang);
    }

    /** Used only if the answer call fails. The table is still shown for multi-row results. */
    private static String fallbackText(List<Map<String, Object>> rows, Lang lang) {
        if (rows.size() > 1) return Msg.ROWS_HEADER.in(lang, rows.size());
        return rows.get(0).entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
    }

    private static String stripFences(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        return t;
    }
}