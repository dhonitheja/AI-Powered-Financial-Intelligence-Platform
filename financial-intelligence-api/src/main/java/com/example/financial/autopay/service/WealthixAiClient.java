package com.example.financial.autopay.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WealthixAiClient {

    private static final Logger log = LoggerFactory.getLogger(WealthixAiClient.class);

    @Value("${wealthix.ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${wealthix.ai.service.secret:}")
    private String internalSecret;

    private final RestTemplate restTemplate;

    public WealthixAiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Analyze autopay schedules via AI service.
     * ONLY send anonymized data — no PII, no account numbers.
     * AI failure must NEVER break schedule operations.
     */
    public Optional<Map> analyzeSchedules(List<Map<String, Object>> schedules) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Wealthix-Service-Secret", internalSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of("schedules", schedules), headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiServiceUrl + "/autopay/analyze",
                    entity,
                    Map.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            // AI failure MUST NOT break schedule creation
            log.warn("[Wealthix] AI service unavailable: {}",
                    e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /**
     * Categorize a payment description.
     * Call after schedule creation if category = CUSTOM.
     */
    public Optional<String> categorizePayment(String description) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Wealthix-Service-Secret", internalSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("description", description), headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiServiceUrl + "/autopay/categorize",
                    entity, Map.class);
            return Optional.ofNullable(
                    (String) response.getBody().get("category"));
        } catch (Exception e) {
            log.warn("[Wealthix] AI categorize unavailable: {}",
                    e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /**
     * Test if the AI service is reachable via /health.
     */
    public boolean isHealthy() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    aiServiceUrl + "/health", Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
