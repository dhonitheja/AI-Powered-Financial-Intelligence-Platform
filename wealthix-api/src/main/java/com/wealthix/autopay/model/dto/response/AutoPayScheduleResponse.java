package com.wealthix.autopay.model.dto.response;

import com.wealthix.autopay.model.entity.PaymentCategory;
import com.wealthix.autopay.model.entity.PaymentFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API response for a single autopay schedule.
 *
 * CRITICAL SECURITY: accountNumberMasked shows ONLY the last 4 digits
 * (e.g. "****1234"). The full account number is NEVER included in any response.
 */
public record AutoPayScheduleResponse(
        UUID id,
        String paymentName,
        PaymentCategory paymentCategory,
        String categoryDisplayName,
        String paymentProvider,
        /** Always "****XXXX" — last 4 digits only. Never full number. */
        String accountNumberMasked,
        boolean hasRoutingNumber,
        boolean hasNotes,
        BigDecimal amount,
        String currency,
        BigDecimal monthlyEquivalent,
        PaymentFrequency frequency,
        LocalDate nextDueDate,
        Integer dueDayOfMonth,
        boolean autoExecute,
        int reminderDaysBefore,
        boolean active,
        String status, // "ACTIVE", "DUE_SOON", "OVERDUE", "INACTIVE"
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
