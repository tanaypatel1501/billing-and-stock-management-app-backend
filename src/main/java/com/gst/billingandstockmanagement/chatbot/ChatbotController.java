package com.gst.billingandstockmanagement.chatbot;

import com.gst.billingandstockmanagement.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final ChatbotRateLimiter rateLimiter;

    @PostMapping("/ask")
    public ResponseEntity<ChatbotResponse> ask(@RequestBody(required = false) ChatbotRequest request) {
        try {
            // Only shop accounts have bills. Answer with a normal 200 and never reach the rate limiter,
            if (!"USER".equalsIgnoreCase(SecurityUtils.getCurrentUserRole())) {
                return ResponseEntity.ok(ChatbotResponse.text("refused",
                        "The sales assistant is only available for shop accounts."));
            }
            Long userId = SecurityUtils.getCurrentUserId();          // identity ONLY from the JWT

            if (request == null) return badRequest("Empty request.");

            // User picked one of the suggested names
            if (request.token() != null && !request.token().isBlank()) {
                if (!rateLimiter.tryAcquire(userId)) return tooMany();
                return ResponseEntity.ok(chatbotService.answerChoice(userId, request.token(), request.choice()));
            }

            String question = request.question() == null ? "" : request.question().trim();
            if (question.isEmpty() || question.length() > 300) {
                return badRequest("Question must be 1-300 characters.");
            }
            if (!rateLimiter.tryAcquire(userId)) return tooMany();

            return ResponseEntity.ok(chatbotService.ask(userId, question, request.history()));

        } catch (Exception e) {
            log.error("Chatbot request failed", e);
            return ResponseEntity.ok(ChatbotResponse.text("failed", Msg.BUSY.in(Lang.ENGLISH)));
        }
    }

    private static ResponseEntity<ChatbotResponse> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ChatbotResponse.text("failed", message));
    }

    private static ResponseEntity<ChatbotResponse> tooMany() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ChatbotResponse.text("failed", "Too many questions. Try again shortly."));
    }
}