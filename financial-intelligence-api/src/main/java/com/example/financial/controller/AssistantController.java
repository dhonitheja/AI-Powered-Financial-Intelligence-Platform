package com.example.financial.controller;

import com.example.financial.service.AIClientService;
import com.example.financial.service.PlaidService;
import com.example.financial.dto.FinancialSummaryDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AIClientService aiClientService;
    private final PlaidService plaidService;

    public AssistantController(AIClientService aiClientService, PlaidService plaidService) {
        this.aiClientService = aiClientService;
        this.plaidService = plaidService;
    }

    public static class ChatRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @PostMapping
    public Mono<ResponseEntity<Map<String, String>>> query(@RequestBody ChatRequest request) {
        String message = request.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "Please provide a message.")));
        }

        String userId = resolveUserId();
        String context = buildFinancialContext(userId);

        return aiClientService.chat(message, context)
                .map(reply -> ResponseEntity.ok(Map.of("reply", reply)))
                .onErrorReturn(ResponseEntity.internalServerError().body(Map.of(
                        "reply", "I'm sorry, I encountered an error while processing your request.")));
    }

    private String buildFinancialContext(String userId) {
        StringBuilder ctx = new StringBuilder();
        try {
            FinancialSummaryDTO summary = plaidService.getFinancialSummary(userId);
            ctx.append(String.format("ACCOUNT SUMMARY:\\n  Total Assets: $%.2f\\n  Credit Utilization: %.1f%%\\n\\n",
                    summary.getTotalAssets(), summary.getCreditUtilization()));
        } catch (Exception e) {
        }

        return ctx.toString();
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";
    }
}
