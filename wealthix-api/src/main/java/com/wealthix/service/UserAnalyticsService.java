package com.wealthix.service;

import com.wealthix.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserAnalyticsService {

    private final TransactionRepository transactionRepository;

    public UserAnalyticsService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Map<String, Double> getSpendingTrends(UUID userId) {
        // Mocking trend data for now or querying if DB has enough
        return new java.util.TreeMap<>(Map.of(
            "2024-11", 1200.0,
            "2024-12", 1500.0,
            "2025-01", 1100.0
        ));
    }

    public List<Map<String, Object>> getTopMerchants(UUID userId) {
        return List.of(
            Map.of("name", "Amazon", "count", 12),
            Map.of("name", "Uber", "count", 8),
            Map.of("name", "Starbucks", "count", 15)
        );
    }

    public double getNetWorth(UUID userId) {
        // Real implementation would sum balances from Plaid accounts
        // For now, simple mock/placeholder
        return 25000.00;
    }
}
