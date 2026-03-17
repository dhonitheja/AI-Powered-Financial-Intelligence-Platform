package com.wealthix.controller;

import com.wealthix.dto.CategorySpendingDTO;
import com.wealthix.dto.ComparisonDTO;
import com.wealthix.repository.TransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Spending comparison and trend analytics")
public class AnalyticsController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AnalyticsController.class);

    private final TransactionRepository transactionRepository;
    private final com.wealthix.repository.AppUserRepository userRepository;

    public AnalyticsController(TransactionRepository transactionRepository,
                               com.wealthix.repository.AppUserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns current period vs previous period spending breakdown by category.
     *
     * @param period "weekly" (Mon–today vs last Mon–Sun) or "monthly" (this month vs last month)
     */
    @GetMapping("/comparison")
    @Operation(summary = "Get spending comparison: current period vs previous period by category")
    public ResponseEntity<ComparisonDTO> getComparison(
            @RequestParam(value = "period", defaultValue = "weekly") String period,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        try {
            com.wealthix.entity.AppUser user = userRepository.findFirstByEmailIgnoreCase(userDetails.getUsername())
                    .or(() -> userRepository.findByUsername(userDetails.getUsername()))
                    .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
            java.util.UUID userId = user.getId();

            LocalDate today = LocalDate.now();
            LocalDate currentStart, currentEnd, previousStart, previousEnd;
            String currentLabel, previousLabel;

            if ("monthly".equalsIgnoreCase(period)) {
                currentStart  = today.withDayOfMonth(1);
                currentEnd    = today;
                LocalDate firstOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
                LocalDate lastOfLastMonth  = today.withDayOfMonth(1).minusDays(1);
                previousStart = firstOfLastMonth;
                previousEnd   = lastOfLastMonth;
                currentLabel  = capitalize(today.getMonth().name()) + " " + today.getYear();
                previousLabel = capitalize(firstOfLastMonth.getMonth().name()) + " " + firstOfLastMonth.getYear();
            } else {
                // Weekly: Monday → today vs previous Monday → Sunday
                LocalDate monday  = today.with(DayOfWeek.MONDAY);
                currentStart  = monday;
                currentEnd    = today;
                previousStart = monday.minusWeeks(1);
                previousEnd   = monday.minusDays(1);
                currentLabel  = "This Week";
                previousLabel = "Last Week";
            }

            List<CategorySpendingDTO> current  = transactionRepository.calculateSpendingByCategoryBetween(userId, currentStart, currentEnd);
            List<CategorySpendingDTO> previous = transactionRepository.calculateSpendingByCategoryBetween(userId, previousStart, previousEnd);

            Map<String, Double> prevMap = previous.stream().collect(
                    Collectors.toMap(
                            CategorySpendingDTO::getCategory,
                            s -> s.getTotalSpending() != null ? Math.abs(s.getTotalSpending()) : 0.0,
                            (a, b) -> a + b));

            double currentTotal  = current.stream()
                    .mapToDouble(s -> s.getTotalSpending() != null ? Math.abs(s.getTotalSpending()) : 0.0)
                    .sum();
            double previousTotal = prevMap.values().stream().mapToDouble(Double::doubleValue).sum();

            List<ComparisonDTO.CategoryComparison> categories = current.stream()
                    .map(s -> {
                        double curr = s.getTotalSpending() != null ? Math.abs(s.getTotalSpending()) : 0.0;
                        double prev = prevMap.getOrDefault(s.getCategory(), 0.0);
                        double changePct = (prev == 0)
                                ? (curr > 0 ? 100.0 : 0.0)
                                : Math.round(((curr - prev) / prev) * 1000.0) / 10.0;
                        return new ComparisonDTO.CategoryComparison(s.getCategory(), curr, prev, changePct);
                    })
                    .sorted((a, b) -> Double.compare(b.currentPeriodSpend(), a.currentPeriodSpend()))
                    .collect(Collectors.toList());

            ComparisonDTO result = new ComparisonDTO();
            result.setPeriod(period);
            result.setCurrentPeriodLabel(currentLabel);
            result.setPreviousPeriodLabel(previousLabel);
            result.setCurrentTotal(currentTotal);
            result.setPreviousTotal(previousTotal);
            result.setCategories(categories);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("CRITICAL ANALYTICS ERROR:", e);
            throw e;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
