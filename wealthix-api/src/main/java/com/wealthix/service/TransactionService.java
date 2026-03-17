package com.wealthix.service;

import com.wealthix.budget.service.BudgetEnforcementService;
import com.wealthix.dto.AIAnalysisResponse;
import com.wealthix.dto.CategorySpendingDTO;
import com.wealthix.entity.AppUser;
import com.wealthix.entity.Transaction;
import com.wealthix.exception.ResourceNotFoundException;
import com.wealthix.repository.AppUserRepository;
import com.wealthix.repository.TransactionRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final AIClientService aiClientService;
    private final NotificationService notificationService;
    private final BudgetEnforcementService budgetEnforcementService;
    private final AppUserRepository appUserRepository;

    public TransactionService(TransactionRepository transactionRepository,
            AIClientService aiClientService,
            NotificationService notificationService,
            BudgetEnforcementService budgetEnforcementService,
            AppUserRepository appUserRepository) {
        this.transactionRepository = transactionRepository;
        this.aiClientService = aiClientService;
        this.notificationService = notificationService;
        this.budgetEnforcementService = budgetEnforcementService;
        this.appUserRepository = appUserRepository;
    }

    // ─── Period Resolver ───────────────────────────────────────────────────────

    /**
     * Converts a named period string to a start timestamp.
     * "weekly" → 7 days ago
     * "monthly" → 30 days ago
     * "6months" → 6 calendar months ago
     * "yearly" → 365 days ago
     * null / "" → null (meaning: no date filter, return all)
     */
    public LocalDate resolveStartDate(String period) {
        if (period == null || period.isBlank())
            return null;
        LocalDate now = LocalDate.now();
        return switch (period.toLowerCase()) {
            case "weekly" -> now.minusDays(7);
            case "monthly" -> now.minusDays(30);
            case "6months" -> now.minusMonths(6);
            case "yearly" -> now.minusDays(365);
            default -> {
                log.warn("Unknown period '{}' – returning all transactions", period);
                yield null;
            }
        };
    }

    /**
     * Maps the public "type" filter param to a list of accountType DB values.
     * "credit" → [CREDIT]
     * "debit" → [CHECKING, SAVINGS]
     * null → null (no type filter)
     */
    public List<String> resolveAccountTypes(String type) {
        if (type == null || type.isBlank())
            return null;
        return switch (type.toLowerCase()) {
            case "credit" -> List.of("CREDIT");
            case "debit" -> List.of("CHECKING", "SAVINGS");
            default -> {
                log.warn("Unknown account type filter '{}' – ignoring", type);
                yield null;
            }
        };
    }

    // ─── CRUD ──────────────────────────────────────────────────────────────────

    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        Objects.requireNonNull(transaction, "Transaction cannot be null");
        log.info("Creating new transaction: {}", transaction.getDescription());
        Transaction saved = transactionRepository.save(transaction);
        analyzeTransactionAsync(saved.getId());
        return saved;
    }

    // ─── Filtered Queries ──────────────────────────────────────────────────────

    /**
     * Returns transactions filtered by optional period AND optional account type.
     * All filtering happens at the DB level via JPQL — nothing filtered on the JVM.
     *
     * @param period "weekly" | "monthly" | "6months" | "yearly" | null
     * @param type   "credit" | "debit" | null
     */
    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions(UUID userId, String period, String type) {
        LocalDate start = resolveStartDate(period);
        List<String> types = resolveAccountTypes(type);
        LocalDate end = LocalDate.now();

        log.info("[TxService] Fetching transactions for user={} period={} type={} (start={} types={})",
                userId, period, type, start, types);

        // ── Route to the correct query variant ────────────────────────────────
        if (start == null && types == null) {
            return transactionRepository.findAll().stream()
                    .filter(t -> t.getUserId().equals(userId))
                    .toList();
        }
        if (start != null && types == null) {
            return transactionRepository.findByDateBetween(userId, start, end);
        }
        if (start == null && types != null) {
            return transactionRepository.findByAccountTypeIn(userId, types);
        }
        return transactionRepository.findByDateBetweenAndAccountTypeIn(userId, start, end, types);
    }

    /** Convenience overload — no type filter. */
    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions(UUID userId, String period) {
        return getAllTransactions(userId, period, null);
    }

    /** Convenience overload — no filters. */
    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions(UUID userId) {
        return getAllTransactions(userId, null, null);
    }

    @Transactional(readOnly = true)
    public Transaction getTransactionById(UUID id) {
        Objects.requireNonNull(id, "Transaction ID cannot be null");
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
    }

    // ─── AI Analysis ───────────────────────────────────────────────────────────

    @Async
    @Transactional
    public void analyzeTransactionAsync(UUID id) {
        log.info("Starting background analysis for transaction: {}", id);
        try {
            Transaction transaction = getTransactionById(id);
            AIAnalysisResponse aiResponse = aiClientService.analyzeTransaction(transaction).block();

            if (aiResponse != null) {
                transaction.setCategory(aiResponse.getCategory());
                transaction.setFraudRiskScore(aiResponse.getFraudRiskScore());
                transaction.setAiExplanation(aiResponse.getExplanation());
                transactionRepository.save(transaction);
                log.info("Analysis completed for transaction: {}", id);

                // Resolve real user email for notifications
                String userEmail = appUserRepository.findById(transaction.getUserId())
                        .map(AppUser::getEmail)
                        .orElse(null);

                if (aiResponse.getFraudRiskScore() >= 80) {
                    notificationService.sendHighRiskAlert(userEmail != null ? userEmail : "unknown",
                            transaction.getDescription(), transaction.getAmount(),
                            aiResponse.getExplanation());
                } else if (transaction.getAmount() > 500.0) {
                    notificationService.sendSpendingAnomalyAlert(userEmail != null ? userEmail : "unknown",
                            transaction.getCategory(), transaction.getAmount(),
                            "This transaction is unusually large. " + aiResponse.getExplanation());
                }

                // Check category spending limits
                if (userEmail != null) {
                    budgetEnforcementService.checkBudgets(
                            transaction.getUserId(), aiResponse.getCategory(), userEmail);
                }
            }
        } catch (Exception e) {
            log.error("Failed to analyze transaction {}: {}", id, e.getMessage());
        }
    }

    // ─── Summary ───────────────────────────────────────────────────────────────

    /**
     * Category spending aggregates, filtered by optional period AND optional type.
     * All filtering is DB-level.
     */
    @Transactional(readOnly = true)
    public List<CategorySpendingDTO> getFinancialSummary(UUID userId, String period, String type) {
        LocalDate start = resolveStartDate(period);
        List<String> types = resolveAccountTypes(type);
        LocalDate end = LocalDate.now();

        if (start == null && types == null) {
            return transactionRepository.calculateTotalSpendingByCategory(userId);
        }
        if (start != null && types == null) {
            return transactionRepository.calculateSpendingByCategoryBetween(userId, start, end);
        }
        if (start == null && types != null) {
            return transactionRepository.calculateSpendingByCategoryForTypes(userId, types);
        }
        return transactionRepository.calculateSpendingByCategoryBetweenForTypes(userId, start, end, types);
    }

    /** Convenience overload — no type filter. */
    @Transactional(readOnly = true)
    public List<CategorySpendingDTO> getFinancialSummary(UUID userId, String period) {
        return getFinancialSummary(userId, period, null);
    }

    /** Convenience overload — no filters. */
    @Transactional(readOnly = true)
    public List<CategorySpendingDTO> getFinancialSummary(UUID userId) {
        return getFinancialSummary(userId, null, null);
    }

    /** Category spending for an explicit date range. */
    @Transactional(readOnly = true)
    public List<CategorySpendingDTO> getFinancialSummaryForRange(UUID userId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.calculateSpendingByCategoryBetween(userId, startDate, endDate);
    }
}
