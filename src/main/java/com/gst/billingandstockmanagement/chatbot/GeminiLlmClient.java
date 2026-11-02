package com.gst.billingandstockmanagement.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiLlmClient implements LlmClient {

    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final RestTemplate rest;
    private final String apiKey;
    private final List<String> models;

    public GeminiLlmClient(@Value("${chatbot.llm.gemini.api-key:}") String apiKey,
                           @Value("${chatbot.llm.gemini.model:}") String modelList) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(20_000);
        this.rest = new RestTemplate(factory);
        this.apiKey = apiKey;
        this.models = Arrays.stream(modelList.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Override
    public String completeJson(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank() || models.isEmpty()) {
            throw new LlmException("Gemini is not configured (GEMINI_API_KEY / GEMINI_MODEL).");
        }

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of("temperature", 0, "responseMimeType", "application/json")
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        LlmException last = null;
        for (String model : models) {
            try {
                return call(model, new HttpEntity<>(body, headers));
            } catch (LlmException e) {
                log.warn("Gemini model '{}' failed: {}", model, e.getMessage());
                last = e;
            }
        }
        throw last;
    }

    private String call(String model, HttpEntity<Map<String, Object>> request) {
        try {
            JsonNode res = rest.postForObject(BASE + model + ":generateContent", request, JsonNode.class);
            String text = res == null ? "" : res.path("candidates").path(0)
                    .path("content").path("parts").path(0).path("text").asText("");
            if (text.isBlank()) throw new LlmException("Empty response from model.");
            return text;
        } catch (HttpStatusCodeException e) {
            throw new LlmException("HTTP " + e.getStatusCode().value(), e);   // never log key or URL
        } catch (RestClientException e) {
            throw new LlmException("Unreachable or timed out: " + e.getClass().getSimpleName(), e);
        }
    }
}