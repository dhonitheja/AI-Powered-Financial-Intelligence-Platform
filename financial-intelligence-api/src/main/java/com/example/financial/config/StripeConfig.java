package com.example.financial.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Initialises the Stripe Java SDK with the secret key at startup.
 *
 * <p>
 * Security:
 * <ul>
 * <li>The key is injected from an environment variable — never hard-coded.</li>
 * <li>Application fails fast if the key is missing or invalid format.</li>
 * <li>Key value is NEVER logged — only whether configuration succeeded.</li>
 * </ul>
 */
@Configuration
public class StripeConfig {

    private static final Logger log = LoggerFactory.getLogger(StripeConfig.class);

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.publishable-key}")
    private String publishableKey;

    @PostConstruct
    public void init() {
        // Fail fast: reject misconfigured or missing keys before any request hits
        // Stripe
        if (secretKey == null || secretKey.isBlank() || secretKey.equals("sk_test_placeholder")) {
            throw new IllegalStateException(
                    "[Stripe] Secret key is not configured. " +
                            "Set STRIPE_SECRET_KEY in your environment variables.");
        }
        if (!secretKey.startsWith("sk_test_") && !secretKey.startsWith("sk_live_")) {
            throw new IllegalStateException(
                    "[Stripe] Invalid secret key format — must start with 'sk_test_' or 'sk_live_'.");
        }

        Stripe.apiKey = secretKey;

        // Log ONLY the key mode (test vs live) and truncated ID — never the full key
        String keyMode = secretKey.startsWith("sk_live_") ? "LIVE" : "TEST";
        log.info("[Stripe] Configured in {} mode. Publishable key prefix: {}",
                keyMode,
                publishableKey != null && publishableKey.length() > 10
                        ? publishableKey.substring(0, 10) + "..."
                        : "(not set)");
    }

    /**
     * Exposes publishable key for endpoints that need to return it to the frontend.
     */
    public String getPublishableKey() {
        return publishableKey;
    }
}
