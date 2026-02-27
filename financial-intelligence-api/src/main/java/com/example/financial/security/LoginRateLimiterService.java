package com.example.financial.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory per-IP rate limiter for login attempts.
 * Allows a maximum of MAX_ATTEMPTS per WINDOW_MINUTES per IP address.
 * Automatically evicts counters after the window expires.
 */
@Service
public class LoginRateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiterService.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final int WINDOW_MINUTES = 1;

    // Caffeine cache: key = IP, value = attempt counter, auto-expires after 1
    // minute
    private final Cache<String, AtomicInteger> attemptCache = Caffeine.newBuilder()
            .expireAfterWrite(WINDOW_MINUTES, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    /**
     * Records a failed login attempt for the given IP.
     */
    public void recordFailedAttempt(String ip) {
        AtomicInteger attempts = attemptCache.get(ip, k -> new AtomicInteger(0));
        int count = attempts.incrementAndGet();
        log.warn("[RateLimit] Failed login from IP {} — attempt {}/{}", ip, count, MAX_ATTEMPTS);
    }

    /**
     * Resets the attempt counter for the given IP on successful login.
     */
    public void resetAttempts(String ip) {
        attemptCache.invalidate(ip);
        log.debug("[RateLimit] Reset attempts for IP {}", ip);
    }

    /**
     * Returns true if the IP is currently rate-limited (has exceeded MAX_ATTEMPTS).
     */
    public boolean isBlocked(String ip) {
        AtomicInteger attempts = attemptCache.getIfPresent(ip);
        return attempts != null && attempts.get() >= MAX_ATTEMPTS;
    }

    /**
     * Returns how many attempts have been made in the current window.
     */
    public int getAttemptCount(String ip) {
        AtomicInteger attempts = attemptCache.getIfPresent(ip);
        return attempts != null ? attempts.get() : 0;
    }
}
