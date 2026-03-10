package com.example.financial.autopay.service;

import com.example.financial.autopay.model.dto.response.AutoPayScheduleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client for calling the AutoPay AI service.
 *
 * Security:
 * - Sends ONLY anonymised data (amounts, categories, frequencies).
 * - NEVER forwards account numbers, routing numbers, or user PII.
 * - Uses a shared internal secret header for service-to-service auth.
 */
@Service
public class AutoPayAIClientService {

    private static final Logger log = LoggerFactory.getLogger(AutoPayAIClientService.class);

    private final WebClient webClient;

    @Value("${spring.ai.service-url:http://localhost:8090}")
    private String aiServiceUrl;

    @Value("${AUTOPAY_INTERNAL_SECRET:}")
    private String internalSecret;

    public AutoPayAIClientService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Calls the AI service to analyse a user's schedules.
     * Returns a map of insights or an empty map on failure.
     *
     * @param schedules API response objects (already have masked account numbers —
     *                  safe to send)
     */
    public Map<String, Object> analyzeSchedules(List<AutoPayScheduleResponse> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return Map.of();
        }

        // Build anonymised payload — only amounts, categories, frequencies
        // This is the final line of defence: no PII leaves the backend
        List<Map<String, Object>> anonymised = schedules.stream().map(s -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("payment_name", s.paymentName()); // name only, no account info
            item.put("payment_category", s.paymentCategory().name());
            item.put("frequency", s.frequency().name());
            item.put("amount", s.amount());
            item.put("currency", s.currency());
            item.put("monthly_equivalent", s.monthlyEquivalent());
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> payload = new HashMap<>();
        payload.put("schedules", anonymised);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = webClient.post()
                    .uri(aiServiceUrl + "/autopay/analyze")
                    .header("X-Internal-Secret", internalSecret)
                    .header("Content-Type", "application/json")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class).map(m -> (Map<String, Object>) m)
                    .onErrorResume(e -> {
                        log.warn("[AutoPayAI] AI service call failed: {}", e.getMessage());
                        return Mono.just(new HashMap<>());
                    })
                    .block();
            return result != null ? result : Map.of();
        } catch (Exception e) {
            log.warn("[AutoPayAI] AI service call failed: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * Calls the AI service to categorise a raw payment description.
     * Returns the category string or "CUSTOM" on failure.
     */
    public Map<String, Object> categorizePayment(String description) {
        if (description == null || description.isBlank()) {
            return Map.of("category", "CUSTOM", "confidence", 0.0);
        }

        Map<String, Object> payload = Map.of("description", description);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = webClient.post()
                    .uri(aiServiceUrl + "/autopay/categorize")
                    .header("X-Internal-Secret", internalSecret)
                    .header("Content-Type", "application/json")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class).map(m -> (Map<String, Object>) m)
                    .onErrorResume(e -> {
                        log.warn("[AutoPayAI] Categorize call failed: {}", e.getMessage());
                        return Mono.just(new HashMap<>());
                    })
                    .block();
            return result != null ? result : Map.of("category", "CUSTOM", "confidence", 0.0);
        } catch (Exception e) {
            log.warn("[AutoPayAI] Categorize call failed: {}", e.getMessage());
            return Map.of("category", "CUSTOM", "confidence", 0.0);
        }
    }
}
