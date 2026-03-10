package com.example.financial.autopay.service;

import com.example.financial.autopay.model.entity.AutoPayExecutionLog;
import com.example.financial.autopay.model.entity.PlaidVerificationStatus;
import com.example.financial.autopay.repository.AutoPayExecutionLogRepository;
import com.example.financial.entity.Transaction;
import com.example.financial.entity.UserBankConnection;
import com.example.financial.repository.TransactionRepository;
import com.example.financial.repository.UserBankConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Verifies Stripe-executed AutoPay payments by cross-referencing them against
 * Plaid transaction data synced from the user's bank.
 *
 * <p>
 * Design:
 * <ul>
 * <li>Called nightly after Plaid sync, and on-demand after a user's sync.</li>
 * <li>Matches by: amount within ±2% AND transaction date after execution.</li>
 * <li>Sets {@code plaid_verification_status = VERIFIED} if matched.</li>
 * <li>Sets {@code plaid_verification_status = NEEDS_REVIEW} if unverified after
 * {@code reviewThresholdDays} days.</li>
 * </ul>
 *
 * <p>
 * Security: This service reads only amounts — no account numbers or sensitive
 * fields are processed or logged.
 */
@Service
public class PlaidVerificationService {

    private static final Logger log = LoggerFactory.getLogger(PlaidVerificationService.class);

    /** Fractional tolerance for amount matching. */
    private static final double AMOUNT_TOLERANCE = 0.02; // 2 %

    private final AutoPayExecutionLogRepository executionLogRepository;
    private final TransactionRepository transactionRepository;
    private final UserBankConnectionRepository connectionRepository;

    public PlaidVerificationService(
            AutoPayExecutionLogRepository executionLogRepository,
            TransactionRepository transactionRepository,
            UserBankConnectionRepository connectionRepository) {
        this.executionLogRepository = executionLogRepository;
        this.transactionRepository = transactionRepository;
        this.connectionRepository = connectionRepository;
    }

    // ── Nightly batch verification ─────────────────────────────────────────────

    /**
     * Called nightly by {@link PlaidVerificationJob}.
     * Scans all UNVERIFIED SUCCESS logs older than {@code reviewThresholdDays}.
     * Either verifies them (if a matching Plaid transaction is found) or flags
     * them as NEEDS_REVIEW.
     *
     * @param reviewThresholdDays days after execution before escalating to
     *                            NEEDS_REVIEW
     */
    @Transactional
    public void runNightlyVerification(int reviewThresholdDays) {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(reviewThresholdDays);
        List<AutoPayExecutionLog> pending = executionLogRepository
                .findUnverifiedSuccessfulBefore(cutoff);

        log.info("[PlaidVerify] Nightly run: {} logs to process (threshold: {}d)",
                pending.size(), reviewThresholdDays);

        for (AutoPayExecutionLog execLog : pending) {
            try {
                processLog(execLog, true);
            } catch (Exception e) {
                log.warn("[PlaidVerify] Failed to process log {}: {}", execLog.getId(), e.getMessage());
            }
        }
    }

    /**
     * Called on-demand when a user triggers a Plaid sync.
     * Only checks that user's recent UNVERIFIED logs.
     *
     * @param userId the authenticated user's UUID
     */
    @Transactional
    public void verifyForUser(UUID userId) {
        OffsetDateTime after = OffsetDateTime.now().minusDays(30);
        List<AutoPayExecutionLog> pending = executionLogRepository
                .findUnverifiedForUser(userId, after);

        log.info("[PlaidVerify] On-demand verification for user {}: {} logs",
                userId, pending.size());

        for (AutoPayExecutionLog execLog : pending) {
            try {
                processLog(execLog, false);
            } catch (Exception e) {
                log.warn("[PlaidVerify] Failed to process log {}: {}", execLog.getId(), e.getMessage());
            }
        }
    }

    // ── Private logic ──────────────────────────────────────────────────────────

    private void processLog(AutoPayExecutionLog execLog, boolean escalateIfNotFound) {
        UUID userId = execLog.getUser().getId();
        BigDecimal amount = execLog.getAmountPaid();
        LocalDateTime executionTime = execLog.getExecutionDate().toLocalDateTime();

        List<String> accountIds = getAccountIds(userId.toString());
        if (accountIds.isEmpty()) {
            return; // No connected accounts — can't verify
        }

        double amt = amount.doubleValue();
        double min = amt * (1 - AMOUNT_TOLERANCE);
        double max = amt * (1 + AMOUNT_TOLERANCE);

        // Look for a matching transaction after the execution date
        List<Transaction> candidates = transactionRepository.findByAmountRangeAfter(
                accountIds, min, max, executionTime);

        if (!candidates.isEmpty()) {
            // Take the closest match by amount
            Transaction match = candidates.stream()
                    .min(Comparator.comparingDouble(t -> Math.abs(Math.abs(t.getAmount()) - amt)))
                    .orElse(candidates.get(0));

            execLog.setPlaidVerificationStatus(PlaidVerificationStatus.VERIFIED);
            execLog.setPlaidMatchedTransactionId(match.getPlaidTransactionId());
            executionLogRepository.save(execLog);

            log.info("[PlaidVerify] Log {} VERIFIED via Plaid tx {}",
                    execLog.getId(), match.getPlaidTransactionId());
        } else if (escalateIfNotFound) {
            execLog.setPlaidVerificationStatus(PlaidVerificationStatus.NEEDS_REVIEW);
            executionLogRepository.save(execLog);
            log.warn("[PlaidVerify] Log {} escalated to NEEDS_REVIEW — no Plaid match found",
                    execLog.getId());
        }
        // else: on-demand call, just leave as UNVERIFIED for now
    }

    private List<String> getAccountIds(String userId) {
        return connectionRepository.findByUserId(userId).stream()
                .map(UserBankConnection::getAccountId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }
}
