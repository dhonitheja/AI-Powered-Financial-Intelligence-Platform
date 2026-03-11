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
public class SpendingForecastService {

    private static final Logger log = LoggerFactory.getLogger(SpendingForecastService.class);

    private final AutoPayScheduleRepository scheduleRepository;
    private final RestTemplate restTemplate;

    @Value("${wealthix.ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${wealthix.ai.service.secret:}")
    private String internalSecret;

    public SpendingForecastService(AutoPayScheduleRepository scheduleRepository, RestTemplate restTemplate) {
        this.scheduleRepository = scheduleRepository;
        this.restTemplate = restTemplate;
    }

    public Object getForecast(AppUser user) {
        List<AutoPaySchedule> schedules = scheduleRepository.findByUserIdAndActiveTrue(user.getId());

        double monthTotal = 0;
        for (AutoPaySchedule s : schedules) {
            monthTotal += s.getAmount().doubleValue();
        }

        // Fake 6 months of historical data based on current total
        List<Map<String, Object>> monthlyData = new ArrayList<>();
        double variance = monthTotal * 0.1;

        for (int i = 0; i < 6; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("month", i);
            // Add a little randomness so regression works
            point.put("total", monthTotal + (Math.random() * variance * 2 - variance));
            monthlyData.add(point);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("monthly_data", monthlyData);
        body.put("forecast_months", 6);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Wealthix-Service-Secret", internalSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Object> response = restTemplate.postForEntity(
                    aiServiceUrl + "/forecast/forecast", request, Object.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("[AI] Spending forecast failed", e);
            return Map.of(
                    "forecasts", List.of(),
                    "trend", "UNKNOWN",
                    "confidence", 0.0,
                    "total_projected", 0.0);
        }
    }
}
