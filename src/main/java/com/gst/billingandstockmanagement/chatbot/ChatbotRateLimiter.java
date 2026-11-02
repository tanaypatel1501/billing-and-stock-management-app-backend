package com.gst.billingandstockmanagement.chatbot;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/** Simple in-memory per-user limiter (fine for a single backend instance). */
@Component
public class ChatbotRateLimiter {

    private static final int PER_MINUTE = 6;
    private static final int PER_DAY = 100;
    private static final long MINUTE = 60_000L;
    private static final long DAY = 86_400_000L;

    private final ConcurrentHashMap<Long, Deque<Long>> hits = new ConcurrentHashMap<>();

    public boolean tryAcquire(Long userId) {
        long now = System.currentTimeMillis();
        Deque<Long> q = hits.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > DAY) q.pollFirst();
            long lastMinute = q.stream().filter(t -> now - t < MINUTE).count();
            if (lastMinute >= PER_MINUTE || q.size() >= PER_DAY) return false;
            q.addLast(now);
            return true;
        }
    }
}