package com.example.financial.autopay.model.dto.response;

/**
 * Masked payment method details returned to the frontend.
 *
 * <p>
 * Security contract: contains ONLY the last 4 digits, brand, and expiry.
 * Full card numbers, CVVs, and fingerprints are NEVER returned.
 */
public record PaymentMethodResponse(
        /** Stripe PaymentMethod ID (e.g. "pm_xxx"). Safe to return — not sensitive. */
        String paymentMethodId,

        /** Card brand: "visa", "mastercard", "amex", "discover", etc. */
        String brand,

        /** Last 4 digits of card number. */
        String last4,

        /** Expiry month (1–12). */
        int expMonth,

        /** Expiry year (4-digit). */
        int expYear,

        /** Whether this is the user's default payment method. */
        boolean isDefault) {
}
