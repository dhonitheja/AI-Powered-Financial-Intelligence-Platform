package com.wealthix.service;

import com.wealthix.repository.AppUserRepository;
import com.wealthix.repository.TransactionRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class AdminMetricsService {

    private final TransactionRepository transactionRepository;
    private final AppUserRepository userRepository;

    public AdminMetricsService(TransactionRepository transactionRepository, AppUserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public BigDecimal getTotalVolume() {
        Double sum = transactionRepository.sumAllAmounts();
        return sum != null ? BigDecimal.valueOf(sum) : BigDecimal.ZERO;
    }

    public double getUserRetention() {
        long totalUsers = userRepository.count();
        if (totalUsers == 0) return 0.0;
        long activeUsers = userRepository.countByLastLoginAtAfter(java.time.LocalDateTime.now().minusDays(30));
        return (double) activeUsers / totalUsers * 100.0;
    }

    public Map<String, String> getSystemHealth() {
        return Map.of(
            "database", "UP",
            "ai_service", "UP",
            "status", "HEALTHY"
        );
    }
}
