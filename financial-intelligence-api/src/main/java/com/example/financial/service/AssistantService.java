package com.example.financial.service;

import com.example.financial.dto.AssistantRequest;
import com.example.financial.dto.AssistantResponse;
import com.example.financial.repository.TransactionRepository;
import com.example.financial.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssistantService {
    private static final Logger logger = LoggerFactory.getLogger(AssistantService.class);

    private final TransactionRepository transactionRepository;
    private final WebClient aiWebClient;

    public AssistantService(TransactionRepository transactionRepository, WebClient aiWebClient) {
        this.transactionRepository = transactionRepository;
        this.aiWebClient = aiWebClient;
    }

    public Mono<AssistantResponse> queryAssistant(String query) {
        logger.info("Processing assistant query: {}", query);

        // Fetch last 10 transactions for context
        List<Transaction> transactions = transactionRepository.findAll();
        String context = transactions.stream()
                .limit(20)
                .map(t -> String.format("%s: $%.2f (%s)", t.getDescription(), t.getAmount(), t.getCategory()))
                .collect(Collectors.joining(", "));

        AssistantRequest request = new AssistantRequest(query, context);

        return aiWebClient.post()
                .uri("/assistant/analyze")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AssistantResponse.class)
                .doOnError(e -> logger.error("Error calling AI Assistant service", e))
                .onErrorReturn(new AssistantResponse(
                        "I'm sorry, I encountered an error while processing your request. Please try again later.",
                        0.0,
                        List.of("Tell me about my spending", "What is my risk?")));
    }
}
