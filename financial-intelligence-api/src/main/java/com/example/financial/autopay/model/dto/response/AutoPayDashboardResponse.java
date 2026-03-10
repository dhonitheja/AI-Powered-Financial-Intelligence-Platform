package com.example.financial.autopay.model.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Dashboard summary response: one call gives the user the full picture
 * of their recurring financial obligations.
 */
public record AutoPayDashboardResponse(
        /** Sum of all active schedule monthly equivalents. */
        BigDecimal totalMonthlyObligations,
        /** totalMonthlyObligations × 12. */
        BigDecimal totalAnnualObligations,
        /** Number of active schedules. */
        int activeScheduleCount,
        /** Payments due in the next 7 days. */
        int dueSoonCount,
        /** Payments with next_due_date < today. */
        int overdueCount,
        /** Category → {count, monthlyAmount}. */
        Map<String, CategoryStat> categoryBreakdown,
        /** Upcoming payments (next 30 days), sorted by due date. */
        List<AutoPayScheduleResponse> upcomingPayments,
        /** 0–100 health score. */
        int paymentHealthScore,
        /** Human-readable health label: "Excellent", "Good", "Fair", "At Risk". */
        String healthLabel) {
    public record CategoryStat(int count, BigDecimal monthlyTotal) {
    }
}
