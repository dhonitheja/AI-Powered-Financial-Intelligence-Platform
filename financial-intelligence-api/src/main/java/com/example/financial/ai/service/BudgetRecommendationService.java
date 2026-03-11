package com.example.financial.ai.service;

import com.example.financial.autopay.model.entity.AutoPaySchedule;
import com.example.financial.autopay.repository.AutoPayScheduleRepository;
import com.example.financial.entity.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class BudgetRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(BudgetRecommendationService.class);

    private final AutoPayScheduleRepository scheduleRepository;
    private final RestTemplate restTemplate;

    @Value("${wealthix.ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${wealthix.ai.service.secret:}")
    private String internalSecret;

    public BudgetRecommendationService(AutoPayScheduleRepository scheduleRepository, RestTemplate restTemplate) {
        this.scheduleRepository = scheduleRepository;
        this.restTemplate = restTemplate;
    }

    public Object getRecommendations(AppUser user) {
        List<AutoPaySchedule> schedules = scheduleRepository.findByUserIdAndActiveTrue(user.getId());

        Map<String, Double> obligations = new HashMap<>();
        for (AutoPaySchedule s : schedules) {
            String category = s.getPaymentCategory().name();
            obligations.put(category, obligations.getOrDefault(category, 0.0)
                    + s.getAmount().doubleValue());
        }

        Map<String, Object> body = new HashMap<>();
        // In a real app we'd fetch the user's monthly income here
        body.put("monthly_income", 5000.0);
        body.put("current_obligations", obligations);
        body.put("savings_goals", List.of());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Wealthix-Service-Secret", internalSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Object> response = restTemplate.postForEntity(
                    aiServiceUrl + "/budget/budget", request, Object.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("[AI] Budget recommendations failed", e);
            return Map.of(
                    "recommendations", List.of(),
                    "suggested_budget", Map.of(),
                    "savings_potential", 0.0,
                    "debt_payoff_plan", List.of());
        }
    }
}
