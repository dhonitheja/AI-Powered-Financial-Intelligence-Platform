package com.example.financial.budget.service;

import com.example.financial.budget.entity.BudgetPeriod;
import com.example.financial.budget.entity.CategoryBudget;
import com.example.financial.budget.repository.CategoryBudgetRepository;
import com.example.financial.dto.CategorySpendingDTO;
import com.example.financial.notification.entity.NotificationType;
import com.example.financial.notification.service.NotificationService;
import com.example.financial.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BudgetEnforcementService {

    private static final Logger log = LoggerFactory.getLogger(BudgetEnforcementService.class);

    private final CategoryBudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    public BudgetEnforcementService(CategoryBudgetRepository budgetRepository,
                                    TransactionRepository transactionRepository,
                                    NotificationService notificationService) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
    }

    /**
     * Check a specific category budget for the user after a single transaction is saved.
     * Called asynchronously after AI analysis sets the transaction category.
     */
    @Async
    public void checkBudgets(UUID userId, String category, String userEmail) {
        if (category == null || category.isBlank() || userEmail == null) return;

        List<CategoryBudget> budgets = budgetRepository.findByUserIdAndActiveTrue(userId);

        for (CategoryBudget budget : budgets) {
            if (!budget.getCategory().equalsIgnoreCase(category)) continue;
            evaluateBudget(budget, userEmail);
        }
    }

    /**
     * Check ALL category budgets for a user after a Plaid sync completes.
     * Called asynchronously from BankSyncService.
     */
    @Async
    public void checkAllBudgets(UUID userId, String userEmail) {
        if (userEmail == null) return;

        List<CategoryBudget> budgets = budgetRepository.findByUserIdAndActiveTrue(userId);
        for (CategoryBudget budget : budgets) {
            evaluateBudget(budget, userEmail);
        }
    }

    private void evaluateBudget(CategoryBudget budget, String userEmail) {
        try {
            LocalDateTime periodStart = resolvePeriodStart(budget.getPeriod());
            LocalDateTime now = LocalDateTime.now();

            List<CategorySpendingDTO> spending =
                    transactionRepository.calculateSpendingByCategoryBetween(periodStart, now);

            double currentSpend = spending.stream()
                    .filter(s -> budget.getCategory().equalsIgnoreCase(s.getCategory()))
                    .mapToDouble(s -> Math.abs(s.getTotalSpending()))
                    .findFirst()
                    .orElse(0.0);

            log.info("[BudgetCheck] userId={} category={} period={} spend={:.2f} limit={:.2f}",
                    budget.getUserId(), budget.getCategory(), budget.getPeriod(),
                    currentSpend, budget.getLimitAmount());

            if (currentSpend >= budget.getLimitAmount()) {
                String periodLabel = budget.getPeriod() == BudgetPeriod.WEEKLY ? "weekly" : "monthly";
                String message = String.format(
                        "⚠️ Budget alert: %s spending is $%.2f this %s, exceeding your $%.2f limit.",
                        budget.getCategory().replace("_", " "),
                        currentSpend,
                        periodLabel,
                        budget.getLimitAmount()
                );
                notificationService.sendNotification(userEmail, message, NotificationType.ALERT);
                log.info("[BudgetCheck] Alert sent to {} for category {}", userEmail, budget.getCategory());
            }
        } catch (Exception e) {
            log.error("[BudgetCheck] Error evaluating budget for category {}: {}", budget.getCategory(), e.getMessage());
        }
    }

    private LocalDateTime resolvePeriodStart(BudgetPeriod period) {
        LocalDate today = LocalDate.now();
        if (period == BudgetPeriod.WEEKLY) {
            // Start of current ISO week (Monday)
            return today.with(DayOfWeek.MONDAY).atStartOfDay();
        } else {
            // Start of current calendar month
            return today.withDayOfMonth(1).atStartOfDay();
        }
    }
}
