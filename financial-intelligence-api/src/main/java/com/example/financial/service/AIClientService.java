package com.example.financial.service;

import com.example.financial.dto.AIAnalysisResponse;
import com.example.financial.entity.Transaction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * AIClientService — wraps OpenAI GPT-4o-mini for two use cases:
 * 1. analyzeTransaction() → structured JSON for fraud detection +
 * categorisation
 * 2. chat() → open-ended financial Q&A with context injection
 *
 * Uses Spring WebClient for non-blocking async calls.
 * All errors fall back gracefully — never throws to the caller.
 */
@Service
public class AIClientService {

    private static final Logger log = LoggerFactory.getLogger(AIClientService.class);

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${openai.max-tokens:800}")
    private int maxTokens;

    @Value("${openai.temperature:0.3}")
    private double temperature;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AIClientService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    // ─── Transaction Analysis ──────────────────────────────────────────────────

    /**
     * Analyzes a transaction for fraud risk score and category.
     * Sends a system+user prompt to GPT-4o-mini and parses the structured JSON
     * response.
     */
    public Mono<AIAnalysisResponse> analyzeTransaction(Transaction transaction) {
        log.info("[AI] Analyzing transaction: {} (${}))", transaction.getDescription(), transaction.getAmount());

        String systemPrompt = """
                You are a financial fraud detection expert. Analyze the provided transaction and respond ONLY with a valid JSON object.
                Do not include markdown code blocks or any text outside the JSON.
                The JSON must match this exact schema:
                {
                  "category": "<one of: Food, Shopping, Transport, Entertainment, Groceries, Technology, Health, Utilities, Travel, Other>",
                  "fraudRiskScore": <decimal between 0.0 and 1.0>,
                  "explanation": "<1-2 sentence explanation of the risk assessment>"
                }
                Scoring guide: 0.0-0.2 = normal, 0.2-0.5 = low risk, 0.5-0.8 = suspicious, 0.8-1.0 = high fraud risk.
                """;

        String userPrompt = String.format(
                "Transaction: %s | Amount: $%.2f | Date: %s",
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getTransactionDate().toLocalDate());

        return callOpenAI(systemPrompt, userPrompt, 300, 0.2)
                .map(raw -> parseAnalysisResponse(raw, transaction))
                .onErrorResume(error -> {
                    log.warn("[AI] Analysis failed for transaction {} — using fallback: {}", transaction.getId(),
                            error.getMessage());
                    return Mono.just(AIAnalysisResponse.builder()
                            .category(transaction.getCategory())
                            .fraudRiskScore(0.0)
                            .explanation("AI analysis unavailable: " + error.getMessage())
                            .build());
                });
    }

    // ─── Financial Chat ────────────────────────────────────────────────────────

    /**
     * Open-ended financial assistant chat.
     * The financialContext string is injected into the system prompt so GPT
     * can answer questions about the user's actual spending, accounts, and trends
     * without the userId ever being sent from the browser.
     *
     * @param userMessage      The user's chat message
     * @param financialContext A serialized snapshot of the user's financial state
     */
    public Mono<String> chat(String userMessage, String financialContext) {
        String systemPrompt = String.format("""
                You are Antigravity — an expert, friendly, and concise personal finance assistant.
                Answer questions clearly and helpfully. Keep responses under 200 words.
                Use bullet points for lists. Use $ for currency.
                Never hallucinate numbers — only reference the context below.

                User's current financial context (live data from their accounts):
                ---
                %s
                ---

                If the user asks about something not in the context, say you don't have that data
                and suggest they sync their accounts.
                """, financialContext != null ? financialContext : "No account data available yet.");

        return callOpenAI(systemPrompt, userMessage, maxTokens, temperature)
                .onErrorReturn("I'm sorry, I couldn't process your request right now. Please try again in a moment.");
    }

    // ─── Core HTTP Call ────────────────────────────────────────────────────────

    private Mono<String> callOpenAI(String systemPrompt, String userMessage, int tokens, double temp) {
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", tokens,
                "temperature", temp,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)));

        return webClient.post()
                .uri(baseUrl + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> {
                    try {
                        return json.at("/choices/0/message/content").asText("");
                    } catch (Exception e) {
                        throw new RuntimeException("Unexpected OpenAI response shape: " + json);
                    }
                })
                .doOnError(e -> log.error("[AI] OpenAI call failed: {}", e.getMessage()));
    }

    // ─── Response Parsing ──────────────────────────────────────────────────────

    private AIAnalysisResponse parseAnalysisResponse(String raw, Transaction tx) {
        try {
            // Strip markdown fences if GPT wraps in ```json...```
            String cleaned = raw.replaceAll("```(?:json)?", "").trim();
            JsonNode node = objectMapper.readTree(cleaned);
            return AIAnalysisResponse.builder()
                    .category(node.path("category").asText(tx.getCategory()))
                    .fraudRiskScore(node.path("fraudRiskScore").asDouble(0.0))
                    .explanation(node.path("explanation").asText(""))
                    .build();
        } catch (Exception e) {
            log.warn("[AI] Failed to parse analysis JSON: {} — raw: {}", e.getMessage(), raw);
            return AIAnalysisResponse.builder()
                    .category(tx.getCategory())
                    .fraudRiskScore(0.0)
                    .explanation("Parse error — " + e.getMessage())
                    .build();
        }
    }
}
