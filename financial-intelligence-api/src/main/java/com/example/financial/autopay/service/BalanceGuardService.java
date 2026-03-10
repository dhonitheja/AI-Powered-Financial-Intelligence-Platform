package com.example.financial.autopay.service;

import com.example.financial.autopay.model.entity.AutoPaySchedule;
import com.example.financial.entity.Transaction;
import com.example.financial.entity.UserBankConnection;
import com.example.financial.repository.TransactionRepository;
import com.example.financial.repository.UserBankConnectionRepository;
import com.example.financial.service.EncryptionService;
import com.plaid.client.request.PlaidApi;
import com.plaid.client.model.AccountsBalanceGetRequest;
import com.plaid.client.model.AccountsGetResponse;
import com.plaid.client.model.AccountBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Safety guard that runs two checks before allowing Stripe to execute a
 * payment:
 *
 * <ol>
 * <li><b>Balance Guard</b>: Fetches real-time balance via Plaid. If the
 * available balance is less than (payment_amount × 1.10), the execution
 * is blocked to avoid overdraft.</li>
 * <li><b>Duplicate Guard</b>: Queries the local transactions table for a
 * matching merchant + similar amount in the current billing cycle. If
 * found, the execution is skipped — the bank already auto-debited it.</li>
 * </ol>
 *
 * <p>
 * Security notes:
 * <ul>
 * <li>Balance amounts are NEVER logged — only the boolean result is
 * recorded.</li>
 * <li>Account numbers are NEVER accessed here; Plaid access token is decrypted
 * only for the API call and immediately discarded.</li>
 * </ul>
 */
@Service
public class BalanceGuardService {

    private static final Logger log = LoggerFactory.getLogger(BalanceGuardService.class);

    /** Required buffer above the payment amount (10 %). */
    private static final double BUFFER_MULTIPLIER = 1.10;

    /** Fractional tolerance for duplicate-detection amount matching (±5 %). */
    private static final double DUPLICATE_TOLERANCE = 0.05;

    private final PlaidApi plaidApi;
    private final EncryptionService encryptionService;
    private final UserBankConnectionRepository connectionRepository;
    private final TransactionRepository transactionRepository;

    public BalanceGuardService(
            PlaidApi plaidApi,
            EncryptionService encryptionService,
            UserBankConnectionRepository connectionRepository,
            TransactionRepository transactionRepository) {
        this.plaidApi = plaidApi;
        this.encryptionService = encryptionService;
        this.connectionRepository = connectionRepository;
        this.transactionRepository = transactionRepository;
    }

    // ── Public checks ──────────────────────────────────────────────────────────

    /**
     * Check 1: Is the available balance sufficient to cover the payment + 10%
     * buffer?
     *
     * @param userId   the user's UUID string
     * @param schedule the autopay schedule about to execute
     * @return {@code true} if balance is sufficient (or cannot be determined —
     *         fail-open)
     */
    public boolean hasSufficientBalance(String userId, AutoPaySchedule schedule) {
        double requiredAmount = schedule.getAmount()
                .multiply(BigDecimal.valueOf(BUFFER_MULTIPLIER))
                .doubleValue();

        List<UserBankConnection> connections = connectionRepository.findByUserId(userId);
        if (connections.isEmpty()) {
            // No bank connected — cannot verify. Allow execution (fail-open) but log.
            log.warn("[BalanceGuard] No bank connection for user {} — allowing execution", userId);
            return true;
        }

        // Use the first connection's access token (they share the same encrypted token)
        String encryptedToken = connections.get(0).getEncryptedAccessToken();
        if (encryptedToken == null || encryptedToken.isBlank()) {
            log.warn("[BalanceGuard] Empty access token for user {} — allowing execution", userId);
            return true;
        }

        try {
            String accessToken = encryptionService.decrypt(encryptedToken);
            AccountsBalanceGetRequest req = new AccountsBalanceGetRequest().accessToken(accessToken);
            retrofit2.Response<AccountsGetResponse> response = plaidApi.accountsBalanceGet(req).execute();

            if (!response.isSuccessful() || response.body() == null) {
                log.warn("[BalanceGuard] Plaid balance fetch failed for user {} — allowing execution", userId);
                return true; // Fail-open: don't block payment if Plaid is down
            }

            // Sum available balance across all non-credit accounts
            double totalAvailable = response.body().getAccounts().stream()
                    .filter(a -> !"credit".equalsIgnoreCase(
                            a.getType() != null ? a.getType().getValue() : ""))
                    .mapToDouble(a -> {
                        if (a.getBalances() == null)
                            return 0;
                        Double avail = a.getBalances().getAvailable();
                        return avail != null ? avail : 0;
                    })
                    .sum();

            // Log only the result, not the actual balance
            boolean sufficient = totalAvailable >= requiredAmount;
            log.info("[BalanceGuard] Balance check for schedule '{}': sufficient={} (required ~{}x amount)",
                    schedule.getPaymentName(), sufficient, BUFFER_MULTIPLIER);
            return sufficient;

        } catch (IOException e) {
            log.warn("[BalanceGuard] IOException fetching balance for user {}: {} — allowing execution",
                    userId, e.getMessage());
            return true; // Fail-open
        } catch (Exception e) {
            log.warn("[BalanceGuard] Unexpected error in balance check: {} — allowing execution", e.getMessage());
            return true;
        }
    }

    /**
     * Check 2: Did the bank already auto-debit this payment in the current billing
     * cycle?
     *
     * <p>
     * Looks for a Plaid-synced transaction matching the merchant name and amount
     * (±5%) within the current billing period (cycleDays window ending now).
     *
     * @param userId    the user's UUID string
     * @param schedule  the autopay schedule
     * @param cycleDays billing cycle length in days (30 for monthly, 7 for weekly,
     *                  etc.)
     * @return {@code true} if a duplicate payment was already detected
     */
    public boolean isAlreadyPaidThisCycle(String userId, AutoPaySchedule schedule, int cycleDays) {
        if (schedule.getPaymentName() == null)
            return false;

        List<String> accountIds = connectionRepository.findByUserId(userId).stream()
                .map(UserBankConnection::getAccountId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (accountIds.isEmpty())
            return false;

        LocalDateTime cycleEnd = LocalDateTime.now();
        LocalDateTime cycleStart = cycleEnd.minusDays(cycleDays);

        // Build a short "keyword" from the payment/provider name for fuzzy matching
        String keyword = buildMerchantKeyword(schedule);

        List<Transaction> matches = transactionRepository.findByMerchantInCycle(
                accountIds, keyword, cycleStart, cycleEnd);

        if (matches.isEmpty())
            return false;

        // Verify amount matches within tolerance
        double expectedAmt = schedule.getAmount().doubleValue();
        boolean found = matches.stream().anyMatch(t -> {
            double txAmt = Math.abs(t.getAmount());
            double deviation = Math.abs(txAmt - expectedAmt) / expectedAmt;
            return deviation <= DUPLICATE_TOLERANCE;
        });

        if (found) {
            log.info("[BalanceGuard] Duplicate detected for schedule '{}': bank auto-debit found in last {} days",
                    schedule.getPaymentName(), cycleDays);
        }

        return found;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Builds a short keyword for merchant-name fuzzy matching.
     * Prefers the provider name; falls back to the first significant word of the
     * payment name.
     */
    private String buildMerchantKeyword(AutoPaySchedule schedule) {
        if (schedule.getPaymentProvider() != null && !schedule.getPaymentProvider().isBlank()) {
            return schedule.getPaymentProvider().split("\\s+")[0];
        }
        // Use first word of payment name
        String[] parts = schedule.getPaymentName().split("\\s+");
        return parts.length > 0 ? parts[0] : schedule.getPaymentName();
    }
}
