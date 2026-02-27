package com.example.financial.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-user rate limiter for the /api/ai/chat endpoint.
 * Limits each authenticated user to MAX_MESSAGES per WINDOW_MINUTES.
 * Keys by userId (resolved from JWT) — not by IP, since users may share IPs
 * (NAT).
 */
@Service
public class ChatRateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(ChatRateLimiterService.class);
    private static final int MAX_MESSAGES = 10; // per window
    private static final int WINDOW_MINUTES = 1;

    private final Cache<String, AtomicInteger> messageCache = Caffeine.newBuilder()
            .expireAfterWrite(WINDOW_MINUTES, TimeUnit.MINUTES)
            .maximumSize(50_000)
            .build();

    /**
     * Returns true if the user is allowed to send another message.
     * Increments the counter atomically.
     */
    public boolean isAllowed(String userId) {
        AtomicInteger counter = messageCache.get(userId, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();
        boolean allowed = count <= MAX_MESSAGES;
        if (!allowed) {
            log.warn("[ChatRateLimit] User {} exceeded {} messages/minute (count={})", userId, MAX_MESSAGES, count);
        }
        return allowed;
    }

    /**
     * Returns how many messages the user has sent in the current window.
     */
    public int getMessageCount(String userId) {
        AtomicInteger counter = messageCache.getIfPresent(userId);
        return counter != null ? counter.get() : 0;
    }

    /** Max allowed messages per window. */
    public int getMaxMessages() {
        return MAX_MESSAGES;
    }

    /** Window length in minutes. */
    public int getWindowMinutes() {
        return WINDOW_MINUTES;
    }
}
