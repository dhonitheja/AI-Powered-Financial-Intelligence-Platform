package com.wealthix.service;

import com.wealthix.repository.AppUserRepository;
import com.wealthix.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMetricsServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock AppUserRepository userRepository;
    @InjectMocks AdminMetricsService adminMetricsService;

    @Test
    void getTotalVolume_sumOfAllTransactions() {
        when(transactionRepository.sumAllAmounts()).thenReturn(5000.50);
        
        BigDecimal volume = adminMetricsService.getTotalVolume();
        
        assertThat(volume).isEqualByComparingTo("5000.50");
    }

    @Test
    void getUserRetention_percentageCalculated() {
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByLastLoginAtAfter(any())).thenReturn(85L);
        
        double retention = adminMetricsService.getUserRetention();
        
        assertThat(retention).isEqualTo(85.0);
    }

    @Test
    void getSystemHealth_checksDatabaseAndAiStatus() {
        Map<String, String> health = adminMetricsService.getSystemHealth();
        
        assertThat(health).containsEntry("database", "UP");
        assertThat(health).containsEntry("ai_service", "UP");
        assertThat(health).containsEntry("status", "HEALTHY");
    }
}
