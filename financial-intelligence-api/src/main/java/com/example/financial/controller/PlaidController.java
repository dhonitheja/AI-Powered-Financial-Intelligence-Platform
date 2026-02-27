package com.example.financial.controller;

import com.example.financial.dto.FinancialSummaryDTO;
import com.example.financial.service.BankSyncService;
import com.example.financial.service.PlaidService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/plaid")
public class PlaidController {
    private static final Logger log = LoggerFactory.getLogger(PlaidController.class);

    private final PlaidService plaidService;
    private final BankSyncService bankSyncService;

    public PlaidController(PlaidService plaidService, BankSyncService bankSyncService) {
        this.plaidService = plaidService;
        this.bankSyncService = bankSyncService;
    }

    @PostMapping("/create-link-token")
    public ResponseEntity<Map<String, String>> createLinkToken(
            @RequestBody(required = false) Map<String, String> payload) {
        try {
            // Prefer explicit userId from payload (used by dev UI), otherwise resolve from
            // auth context.
            String userId = (payload != null && payload.get("userId") != null)
                    ? payload.get("userId")
                    : resolveUserId(null);
            String linkToken = plaidService.createLinkToken(userId);
            return ResponseEntity.ok(Map.of("link_token", linkToken));
        } catch (Exception e) {
            log.error("Error creating link token: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/exchange-public-token")
    public ResponseEntity<Map<String, Object>> exchangeToken(
            @RequestBody(required = false) Map<String, String> payload) {
        log.info("[Exchange] Starting exchange...");
        try {
            if (payload == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Request body is missing"));
            }

            // Resolve userId: use payload if present, otherwise fallback to auth context.
            String userId = (payload.get("userId") != null) ? payload.get("userId") : resolveUserId(null);
            String publicToken = payload.get("public_token");

            if (publicToken != null && publicToken.length() >= 10) {
                log.info("[Exchange] Public token received: {}...", publicToken.substring(0, 10));
            } else {
                log.info("[Exchange] Public token received is suspiciously short or null.");
            }

            if (publicToken == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "public_token is required"));
            }
            Map<String, Object> result = plaidService.exchangePublicToken(userId, publicToken);
            log.info("[Exchange] Exact response being sent to frontend: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[Exchange] Error:", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error",
                    e.getMessage() != null ? e.getMessage() : "Failed to exchange token"));
        }
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> sync(@RequestBody(required = false) Map<String, String> payload) {
        try {
            String userId = resolveUserId(payload != null ? payload.get("userId") : null);
            int imported = plaidService.syncTransactions(userId);
            return ResponseEntity.ok(Map.of("success", true, "imported_count", imported));
        } catch (IllegalStateException e) {
            log.warn("Sync error: {}", e.getMessage());
            return ResponseEntity.status(404).body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error syncing transactions: ", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Sync failed: " + e.getMessage()));
        }
    }

    /**
     * Returns the full financial position: bank accounts, credit cards,
     * totalAssets, totalLiabilities, netWorth, creditUtilization.
     */
    @GetMapping("/accounts")
    public ResponseEntity<FinancialSummaryDTO> getAccountSummary(@RequestParam(required = false) String userId) {
        try {
            String resolvedId = resolveUserId(userId);
            FinancialSummaryDTO summary = plaidService.getFinancialSummary(resolvedId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error fetching account summary: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lightweight polling endpoint the frontend calls every 30s.
     * Returns accountCount + lastSyncedAt epoch so the UI can detect
     * new data without the userId ever leaving the server.
     */
    @GetMapping("/sync-status")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        try {
            String userId = resolveUserId(null);
            BankSyncService.SyncStatusDTO status = bankSyncService.getSyncStatus(userId);
            return ResponseEntity.ok(Map.of(
                    "accountCount", status.accountCount(),
                    "lastSyncedAt", status.lastSyncedAt()));
        } catch (Exception e) {
            log.error("Error fetching sync status: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── Resolve user ID from JWT auth context (ignores payload) ────────────────
    private String resolveUserId(String ignoredPayloadUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.example.financial.security.UserDetailsImpl) {
            return ((com.example.financial.security.UserDetailsImpl) auth.getPrincipal()).getId();
        }
        throw new IllegalStateException("User is not authenticated");
    }
}
