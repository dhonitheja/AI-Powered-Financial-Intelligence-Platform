package com.wealthix.controller;

import com.wealthix.ai.model.dto.JassHybridResponseDTO;
import com.wealthix.ai.model.dto.AITransactionDTO;
import com.wealthix.autopay.service.WealthixAiClient;
import com.wealthix.plaid.service.PlaidService;
import com.wealthix.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for the Jass AI Assistant.
 * Handles financial analysis requests and triggers the Hybrid AI Router.
 */
@RestController
@RequestMapping("/api/assistant")
@PreAuthorize("isAuthenticated()")
public class AssistantController {

    private final WealthixAiClient aiClient;
    private final PlaidService plaidService;

    @Autowired
    public AssistantController(WealthixAiClient aiClient, PlaidService plaidService) {
        this.aiClient = aiClient;
        this.plaidService = plaidService;
    }

    /**
     * Endpoint for the Jass Chat Assistant.
     * Takes a user query and returns a Hybrid AI response (Flash + Claude).
     */
    @PostMapping("/chat")
    public ResponseEntity<?> askJass(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam String itemId,
            @RequestParam(required = false) String query) {

        // 1. Fetch the 90-day transaction window from Plaid
        List<AITransactionDTO> transactions = plaidService.getTransactionsForAI(itemId);

        // 2. Route through the Python Hybrid AI Service
        JassHybridResponseDTO response = aiClient.getAdvancedIntelligence(transactions, query);

        // 3. Handle null response (AI service unavailable)
        if (response == null) {
            return ResponseEntity.status(503).body(
                Map.of("error", "AI service temporarily unavailable. Please try again shortly."));
        }

        // 4. Attach a tracking ID for the UI if missing
        if (response.getAnalysisId() == null) {
            response.setAnalysisId(UUID.randomUUID().toString());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Trigger a standard automated audit (Gemini Flash only).
     */
    @GetMapping("/audit/{itemId}")
    public ResponseEntity<?> runStandardAudit(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable String itemId) {
        List<AITransactionDTO> transactions = plaidService.getTransactionsForAI(itemId);
        JassHybridResponseDTO response = aiClient.getAdvancedIntelligence(transactions, "Run standard financial audit.");
        if (response == null) {
            return ResponseEntity.status(503).body(
                Map.of("error", "AI service temporarily unavailable."));
        }
        return ResponseEntity.ok(response);
    }
}
