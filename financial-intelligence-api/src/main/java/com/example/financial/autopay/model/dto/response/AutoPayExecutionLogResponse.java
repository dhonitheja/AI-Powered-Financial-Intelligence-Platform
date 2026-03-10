package com.example.financial.autopay.model.dto.response;

import com.example.financial.autopay.model.entity.ExecutionStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API response for a single execution log entry.
 * Contains NO sensitive financial identifiers.
 */
public record AutoPayExecutionLogResponse(
        UUID id,
        UUID scheduleId,
        String paymentName,
        OffsetDateTime executionDate,
        BigDecimal amountPaid,
        String currency,
        ExecutionStatus status,
        /** Generic message only — no PII. */
        String failureReason,
        String plaidTransactionId,
        OffsetDateTime createdAt) {
}
