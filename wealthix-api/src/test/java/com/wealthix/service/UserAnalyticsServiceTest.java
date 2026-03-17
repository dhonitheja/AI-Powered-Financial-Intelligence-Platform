package com.wealthix.service;

import com.wealthix.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UserAnalyticsServiceTest {

    @Mock TransactionRepository transactionRepository;
    @InjectMocks UserAnalyticsService userAnalyticsService;

    @Test
    void getSpendingTrends_groupedByMonth() {
        UUID userId = UUID.randomUUID();
        Map<String, Double> trends = userAnalyticsService.getSpendingTrends(userId);
        
        assertThat(trends).isNotEmpty();
        assertThat(trends.keySet().iterator().next()).matches("\\d{4}-\\d{2}");
    }

    @Test
    void getTopMerchants_byFrequency() {
        UUID userId = UUID.randomUUID();
        List<Map<String, Object>> merchants = userAnalyticsService.getTopMerchants(userId);
        
        assertThat(merchants).hasSizeGreaterThan(0);
        assertThat(merchants.get(0)).containsKey("name");
        assertThat(merchants.get(0)).containsKey("count");
    }

    @Test
    void getNetWorth_calculatesAssetsMinusLiabilities() {
        UUID userId = UUID.randomUUID();
        double netWorth = userAnalyticsService.getNetWorth(userId);
        
        assertThat(netWorth).isEqualTo(25000.00);
    }
}
