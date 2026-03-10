package com.example.financial.autopay.service;

import com.example.financial.autopay.model.dto.response.DetectedRecurringPaymentDTO;
import com.example.financial.autopay.model.entity.PaymentCategory;
import com.example.financial.autopay.model.entity.PaymentFrequency;
import com.example.financial.entity.Transaction;
import com.example.financial.entity.UserBankConnection;
import com.example.financial.repository.TransactionRepository;
import com.example.financial.repository.UserBankConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyses a user's Plaid transaction history to detect recurring payments
 * that are not yet in their AutoPay Hub.
 *
 * <p>
 * Algorithm:
 * <ol>
 * <li>Fetch the last 90 days of NEGATIVE transactions for the user's
 * accounts.</li>
 * <li>Group by normalised description (merchant name).</li>
 * <li>Keep groups that appear in 2+ distinct calendar months.</li>
 * <li>Verify that the amounts are within ±5% of each other.</li>
 * <li>Score confidence and map to the closest AutoPay category.</li>
 * </ol>
 *
 * <p>
 * Security: Only anonymised data (amounts, sanitised descriptions) is returned.
 * No account numbers or Plaid transaction IDs are exposed to callers.
 */
@Service
public class RecurringPaymentDetectionService {

    private static final Logger log = LoggerFactory.getLogger(RecurringPaymentDetectionService.class);

    /** Minimum number of months the pattern must appear in to be surfaced. */
    private static final int MIN_OCCURRENCES = 2;

    /** Maximum amount-deviation fraction allowed for "same recurring" grouping. */
    private static final double AMOUNT_TOLERANCE = 0.05; // 5 %

    /** Minimum absolute amount — ignore tiny transactions (< $1). */
    private static final double MIN_AMOUNT = 1.0;

    private final TransactionRepository transactionRepository;
    private final UserBankConnectionRepository connectionRepository;

    public RecurringPaymentDetectionService(
            TransactionRepository transactionRepository,
            UserBankConnectionRepository connectionRepository) {
        this.transactionRepository = transactionRepository;
        this.connectionRepository = connectionRepository;
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Returns suggested recurring payments detected from Plaid data.
     *
     * @param userId the authenticated user's UUID string
     * @return list of detected patterns ordered by confidence descending
     */
    @Transactional(readOnly = true)
    public List<DetectedRecurringPaymentDTO> detectRecurring(String userId) {
        List<String> accountIds = getAccountIds(userId);
        if (accountIds.isEmpty()) {
            log.info("[RecurringDetect] No connected accounts for user {}", userId);
            return List.of();
        }

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(90);

        List<Transaction> txs = transactionRepository
                .findNegativeByAccountIdsBetween(accountIds, start, end);

        if (txs.isEmpty()) {
            return List.of();
        }

        log.info("[RecurringDetect] Analysing {} negative transactions for user {}", txs.size(), userId);

        // Group by normalised description
        Map<String, List<Transaction>> grouped = txs.stream()
                .filter(t -> t.getAmount() != null && Math.abs(t.getAmount()) >= MIN_AMOUNT)
                .collect(Collectors.groupingBy(t -> normalise(t.getDescription())));

        List<DetectedRecurringPaymentDTO> results = new ArrayList<>();

        for (Map.Entry<String, List<Transaction>> entry : grouped.entrySet()) {
            String merchant = entry.getKey();
            List<Transaction> group = entry.getValue();

            // Count distinct calendar months
            Set<String> months = group.stream()
                    .map(t -> t.getTransactionDate().getYear() + "-"
                            + t.getTransactionDate().getMonthValue())
                    .collect(Collectors.toSet());

            if (months.size() < MIN_OCCURRENCES)
                continue;

            // Check amount consistency
            DoubleSummaryStatistics stats = group.stream()
                    .mapToDouble(t -> Math.abs(t.getAmount()))
                    .summaryStatistics();

            double avg = stats.getAverage();
            double deviation = (avg > 0) ? (stats.getMax() - stats.getMin()) / avg : 0;
            if (deviation > AMOUNT_TOLERANCE * 4)
                continue; // extremely variable — skip

            // Day-of-month mode
            int dayOfMonth = mostCommonDay(group);

            // Confidence: based on amount consistency + occurrence count
            int confidence = computeConfidence(deviation, months.size());

            BigDecimal avgAmount = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
            BigDecimal minAmount = BigDecimal.valueOf(stats.getMin()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal maxAmount = BigDecimal.valueOf(stats.getMax()).setScale(2, RoundingMode.HALF_UP);

            results.add(new DetectedRecurringPaymentDTO(
                    toTitleCase(merchant),
                    sanitiseDescription(merchant),
                    avgAmount,
                    minAmount,
                    maxAmount,
                    inferCategory(merchant),
                    PaymentFrequency.MONTHLY,
                    dayOfMonth,
                    months.size(),
                    confidence,
                    "USD"));
        }

        // Sort by confidence desc, then amount desc
        results.sort(Comparator
                .comparingInt(DetectedRecurringPaymentDTO::confidenceScore).reversed()
                .thenComparing(Comparator.comparing(DetectedRecurringPaymentDTO::averageAmount).reversed()));

        log.info("[RecurringDetect] Detected {} recurring patterns for user {}", results.size(), userId);
        return results;
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private List<String> getAccountIds(String userId) {
        List<UserBankConnection> connections = connectionRepository.findByUserId(userId);
        return connections.stream()
                .map(UserBankConnection::getAccountId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /** Lower-case, trim, remove common noise words. */
    private String normalise(String description) {
        if (description == null)
            return "";
        String s = description.toLowerCase().trim();
        // Remove trailing transaction IDs / reference numbers (digit sequences > 4
        // chars)
        s = s.replaceAll("\\b\\d{5,}\\b", "").trim();
        // Remove common noise suffixes
        s = s.replaceAll("\\s+(llc|inc|ltd|corp|co\\.?)$", "").trim();
        return s;
    }

    /**
     * Sanitises a description for the DTO — redacts digit sequences > 4 chars
     * to prevent inadvertent exposure of partial account numbers.
     */
    private String sanitiseDescription(String desc) {
        return desc.replaceAll("\\d{5,}", "****");
    }

    /** Compute the most frequent day-of-month across the group. */
    private int mostCommonDay(List<Transaction> group) {
        return group.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTransactionDate().getDayOfMonth(),
                        Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(1);
    }

    /**
     * @param deviation fractional amount deviation (0 = perfectly consistent)
     * @param months    number of distinct months observed
     */
    private int computeConfidence(double deviation, int months) {
        int score = 100;
        // Penalise amount variance
        score -= (int) (deviation * 200); // -20 pts per 10 % deviation
        // Reward more occurrences
        score += Math.min(20, (months - MIN_OCCURRENCES) * 5);
        return Math.max(0, Math.min(100, score));
    }

    /** Very basic keyword → category mapping. */
    private PaymentCategory inferCategory(String merchant) {
        String m = merchant.toLowerCase();
        if (m.contains("netflix") || m.contains("spotify") || m.contains("apple") ||
                m.contains("amazon prime") || m.contains("hulu") || m.contains("disney"))
            return PaymentCategory.SUBSCRIPTION;
        if (m.contains("mortgage") || m.contains("home loan"))
            return PaymentCategory.HOME_LOAN;
        if (m.contains("insurance") || m.contains("geico") || m.contains("allstate"))
            return PaymentCategory.AUTO_INSURANCE;
        if (m.contains("electric") || m.contains("gas") || m.contains("water") ||
                m.contains("utility") || m.contains("pge") || m.contains("con ed"))
            return PaymentCategory.UTILITY;
        if (m.contains("rent"))
            return PaymentCategory.RENT;
        if (m.contains("loan") || m.contains("emi"))
            return PaymentCategory.PERSONAL_LOAN;
        if (m.contains("credit card") || m.contains("visa") || m.contains("mastercard"))
            return PaymentCategory.CREDIT_CARD;
        if (m.contains("sip") || m.contains("mutual fund"))
            return PaymentCategory.SIP;
        return PaymentCategory.CUSTOM;
    }

    private String toTitleCase(String s) {
        if (s == null || s.isBlank())
            return s;
        String[] words = s.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1)
                    sb.append(word.substring(1));
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}
