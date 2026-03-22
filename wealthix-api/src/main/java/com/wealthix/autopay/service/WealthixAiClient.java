package com.wealthix.autopay.service;

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
import java.util.HashMap;
import com.wealthix.ai.model.dto.AITransactionDTO;
import com.wealthix.ai.model.dto.JassHybridResponseDTO;

@Service
public class WealthixAiClient {

    private static final Logger log = LoggerFactory.getLogger(WealthixAiClient.class);

    @Value("${wealthix.ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${wealthix.ai.service.secret}")
    private String internalSecret;

    private final RestTemplate restTemplate;

    public WealthixAiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Hybrid AI Gateway: Gemini 1.5 Flash + Claude 3.5 Sonnet router.
     * Ensures fallback logic if expert strategy fails.
     */
    /**
     * Alias for analyzeTransactions to support legacy or alternative controller patterns.
     */
    public JassHybridResponseDTO getAdvancedIntelligence(List<AITransactionDTO> transactions, String query) {
        return analyzeTransactions("system-triggered", transactions, query).orElse(null);
    }

    public Optional<JassHybridResponseDTO> analyzeTransactions(String userId, List<AITransactionDTO> transactions, String userQuery) {
        String url = aiServiceUrl + "/assistant/analyze";
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Wealthix-Service-Secret", internalSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> request = new HashMap<>();
            request.put("user_id", userId);
            request.put("transactions", transactions);
            request.put("user_query", userQuery != null ? userQuery : "");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<JassHybridResponseDTO> response = restTemplate.postForEntity(
                url, 
                entity, 
                JassHybridResponseDTO.class
            );
            
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.error("[Wealthix] Hybrid AI Router failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<String> categorizePayment(String description) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Wealthix-Service-Secret", internalSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("description", description), headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    aiServiceUrl + "/autopay/categorize",
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("category")) {
                return Optional.of((String) body.get("category"));
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("[Wealthix] AI categorize unavailable: {}",
                    e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /**
     * Send transaction data to AI service for RAG vector storage.
     */
    public void ingestTransactions(String userId, List<AITransactionDTO> transactions) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Wealthix-Service-Secret", internalSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = Map.of(
                "user_id", userId,
                "transactions", transactions
            );
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            restTemplate.exchange(
                    aiServiceUrl + "/assistant/ingest",
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            log.info("[Wealthix] Ingested {} transactions for user {} to AI Service.", transactions.size(), userId);
        } catch (Exception e) {
            log.warn("[Wealthix] AI ingest failed: {}", e.getMessage());
        }
    }

    public boolean isHealthy() {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    aiServiceUrl + "/health",
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
