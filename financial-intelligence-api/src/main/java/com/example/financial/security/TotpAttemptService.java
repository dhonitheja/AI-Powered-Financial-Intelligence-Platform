package com.example.financial.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks TOTP verification attempts per email.
 * <p>
 * Rules:
 * - Maximum 5 attempts per 5-minute window (matching TOTP validity window).
 * - After a SUCCESSFUL verification the counter is cleared.
 * - After MAX_ATTEMPTS the account is locked for the remainder of the 5-minute
 * window.
 */
@Service
public class TotpAttemptService {

    private static final Logger log = LoggerFactory.getLogger(TotpAttemptService.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final int WINDOW_MINUTES = 5;

    // Caffeine cache: key = email, value = attempt counter
    private final Cache<String, AtomicInteger> attemptCache = Caffeine.newBuilder()
            .expireAfterWrite(WINDOW_MINUTES, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    /**
     * Records a failed OTP attempt for the given email.
     * Returns the new attempt count.
     */
    public int recordFailedAttempt(String email) {
        AtomicInteger attempts = attemptCache.get(email, k -> new AtomicInteger(0));
        int count = attempts.incrementAndGet();
        log.warn("[TOTP] Failed OTP attempt for {} — {}/{}", email, count, MAX_ATTEMPTS);
        return count;
    }

    /**
     * Clears the attempt counter after a successful verification.
     */
    public void clearAttempts(String email) {
        attemptCache.invalidate(email);
        log.debug("[TOTP] Cleared attempts for {} after successful verification", email);
    }

    /**
     * Returns true if this email has exceeded the maximum allowed OTP attempts.
     */
    public boolean isLocked(String email) {
        AtomicInteger attempts = attemptCache.getIfPresent(email);
        return attempts != null && attempts.get() >= MAX_ATTEMPTS;
    }

    /**
     * Returns remaining attempts before lockout.
     */
    public int getRemainingAttempts(String email) {
        AtomicInteger attempts = attemptCache.getIfPresent(email);
        int used = (attempts != null) ? attempts.get() : 0;
        return Math.max(0, MAX_ATTEMPTS - used);
    }
}
