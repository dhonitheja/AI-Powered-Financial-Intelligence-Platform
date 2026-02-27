package com.example.financial.controller;

import com.example.financial.dto.FinancialSummaryDTO;
import com.example.financial.security.ChatRateLimiterService;
import com.example.financial.service.AIClientService;
import com.example.financial.service.PlaidService;
import com.example.financial.service.TransactionService;
import com.example.financial.dto.CategorySpendingDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AIChatController — provides the /api/ai/chat endpoint.
 *
 * Security model:
 * - Endpoint requires JWT (protected by
 * WebSecurityConfig.anyRequest().authenticated())
 * - userId is resolved from SecurityContext — never from request body
 * - Per-user rate limiting via ChatRateLimiterService (10 msgs/minute)
 * - Financial context is assembled server-side from live DB data and injected
 * into the system prompt — the browser sends only the user's text message
 */
@RestController
@RequestMapping("/api/ai")
public class AIChatController {

    private static final Logger log = LoggerFactory.getLogger(AIChatController.class);

    private final AIClientService aiClientService;
    private final PlaidService plaidService;
    private final TransactionService transactionService;
    private final ChatRateLimiterService rateLimiter;

    public AIChatController(AIClientService aiClientService,
            PlaidService plaidService,
            TransactionService transactionService,
            ChatRateLimiterService rateLimiter) {
        this.aiClientService = aiClientService;
        this.plaidService = plaidService;
        this.transactionService = transactionService;
        this.rateLimiter = rateLimiter;
    }

    // ─── Chat endpoint ─────────────────────────────────────────────────────────

    /**
     * POST /api/ai/chat
     * Body: { "message": "How much did I spend on food last month?" }
     * Response: { "reply": "...", "remainingMessages": N }
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> body) {
        String userId = resolveUserId();
        String message = body.get("message");

        // ── Validate input ────────────────────────────────────────────────────
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message is required"));
        }
        if (message.length() > 1000) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message too long (max 1000 chars)"));
        }

        // ── Rate limit check ──────────────────────────────────────────────────
        if (!rateLimiter.isAllowed(userId)) {
            int limit = rateLimiter.getMaxMessages();
            int window = rateLimiter.getWindowMinutes();
            return ResponseEntity.status(429).body(Map.of(
                    "error", String.format("Rate limit exceeded — max %d messages per %d minute(s)", limit, window),
                    "retryAfterSeconds", 60));
        }

        // ── Assemble financial context (all server-side, no userId in response) ─
        String context = buildFinancialContext(userId);
        log.info("[Chat] User {} sent message ({} chars), context {} chars", userId, message.length(),
                context.length());

        // ── Call OpenAI ───────────────────────────────────────────────────────
        String reply = aiClientService.chat(message, context).block();

        int remaining = Math.max(0, rateLimiter.getMaxMessages() - rateLimiter.getMessageCount(userId));

        return ResponseEntity.ok(Map.of(
                "reply", reply != null ? reply : "Sorry, I couldn't generate a response.",
                "remainingMessages", remaining));
    }

    // ─── Context Builder ───────────────────────────────────────────────────────

    /**
     * Assembles a plain-text financial context snapshot for the AI system prompt.
     * All data comes from the authenticated user's own DB records.
     * This text is sent to OpenAI — never to the browser.
     */
    private String buildFinancialContext(String userId) {
        StringBuilder ctx = new StringBuilder();

        // Account summary
        try {
            FinancialSummaryDTO summary = plaidService.getFinancialSummary(userId);
            ctx.append(String.format("ACCOUNT SUMMARY:\n"));
            ctx.append(String.format("  Total Assets:       $%.2f\n", summary.getTotalAssets()));
            ctx.append(String.format("  Total Liabilities:  $%.2f\n", summary.getTotalLiabilities()));
            ctx.append(String.format("  Net Worth:          $%.2f\n", summary.getNetWorth()));
            ctx.append(String.format("  Credit Utilization: %.1f%%\n", summary.getCreditUtilization()));
            ctx.append(String.format("  Accounts linked:    %d\n\n", summary.getAccountCount()));
        } catch (Exception e) {
            ctx.append("ACCOUNT SUMMARY: Not available\n\n");
        }

        // Monthly spending by category
        try {
            List<CategorySpendingDTO> monthly = transactionService.getFinancialSummary("monthly", null);
            if (!monthly.isEmpty()) {
                ctx.append("MONTHLY SPENDING BY CATEGORY:\n");
                monthly.forEach(
                        c -> ctx.append(String.format("  %-22s $%.2f\n", c.getCategory() + ":", c.getTotalSpending())));
                ctx.append("\n");
            }
        } catch (Exception e) {
            ctx.append("MONTHLY SPENDING: Not available\n\n");
        }

        // 6-month trends
        try {
            List<CategorySpendingDTO> sixMonths = transactionService.getFinancialSummary("6months", null);
            if (!sixMonths.isEmpty()) {
                double totalSixMonth = sixMonths.stream().mapToDouble(CategorySpendingDTO::getTotalSpending).sum();
                ctx.append(String.format("6-MONTH TOTAL SPENDING: $%.2f\n", Math.abs(totalSixMonth)));
            }
        } catch (Exception e) {
            // ignore
        }

        return ctx.toString();
    }

    // ── Resolve authenticated user from SecurityContext ────────────────────────
    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";
    }
}
