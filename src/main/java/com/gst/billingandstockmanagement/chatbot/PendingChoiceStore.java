package com.gst.billingandstockmanagement.chatbot;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Remembers a validated query while the user picks which "Dr. S K ..." they meant. */
@Component
public class PendingChoiceStore {

    public record Pending(Long userId, String question, String sql, List<String> options, Lang lang, long expiresAt) {}

    private static final long TTL_MS = 10 * 60_000L;
    private static final int MAX_ENTRIES = 5000;

    private final ConcurrentHashMap<String, Pending> map = new ConcurrentHashMap<>();

    /** Returns the token, or null if the store is full. */
    public String put(Long userId, String question, String sql, List<String> options, Lang lang) {
        long now = System.currentTimeMillis();
        if (map.size() >= MAX_ENTRIES) map.values().removeIf(p -> p.expiresAt() < now);
        if (map.size() >= MAX_ENTRIES) return null;

        String token = UUID.randomUUID().toString();
        map.put(token, new Pending(userId, question, sql, List.copyOf(options), lang, now + TTL_MS));
        return token;
    }

    /** One-time use. Returns null if unknown, expired, or owned by a different user. */
    public Pending take(String token, Long userId) {
        if (token == null) return null;
        Pending p = map.remove(token);
        if (p == null || p.expiresAt() < System.currentTimeMillis() || !p.userId().equals(userId)) return null;
        return p;
    }
}