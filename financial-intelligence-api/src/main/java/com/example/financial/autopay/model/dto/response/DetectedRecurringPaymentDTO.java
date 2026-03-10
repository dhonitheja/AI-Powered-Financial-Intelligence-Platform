package com.example.financial.autopay.model.dto.response;

import com.example.financial.autopay.model.entity.PaymentCategory;
import com.example.financial.autopay.model.entity.PaymentFrequency;

import java.math.BigDecimal;

/**
 * A suggested recurring payment detected from Plaid transaction history.
 * NOT saved to the database — presented to the user for confirmation.
 *
 * Security: contains ONLY anonymised transaction metadata (description,
 * amounts).
 * No account numbers, routing numbers, or full transaction IDs are exposed.
 */
public record DetectedRecurringPaymentDTO(

        /** Display name inferred from the merchant name. */
        String paymentName,

        /** Sanitised merchant description — digit sequences > 4 chars redacted. */
        String merchantDescription,

        /** Average amount across detected recurring instances. */
        BigDecimal averageAmount,

        /** Minimum amount seen — helps identify variable bills. */
        BigDecimal minAmount,

        /** Maximum amount seen. */
        BigDecimal maxAmount,

        /** Best-guess category for this payment. */
        PaymentCategory suggestedCategory,

        /** Inferred frequency (always MONTHLY for now; ANNUALLY if 1/yr). */
        PaymentFrequency suggestedFrequency,

        /** Day of month the transaction most commonly appears on. */
        int suggestedDayOfMonth,

        /** Number of months this pattern was detected across. */
        int occurrenceCount,

        /**
         * Confidence score 0–100.
         * ≥ 80 = high confidence (same amount every month).
         * 50–79 = medium (similar amounts).
         * < 50 = low (variable amounts).
         */
        int confidenceScore,

        /** ISO-4217 currency code inferred from amounts (always USD for Plaid US). */
        String currency) {
}
