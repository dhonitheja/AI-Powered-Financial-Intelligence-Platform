package com.example.financial.controller;

import com.example.financial.service.PlaidWebhookService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Receives Plaid webhook events at POST /api/webhooks/plaid.
 *
 * Security model:
 * - Endpoint is permit-all (Plaid cannot authenticate via JWT cookie).
 * - All requests are rejected unless the HMAC-SHA256 signature in the
 * Plaid-Verification header matches the raw request body.
 * - No userId is accepted in the payload — user resolution happens
 * server-side via item_id → UserBankConnection lookup.
 * - Event processing is fully async (non-blocking response to Plaid).
 */
@RestController
@RequestMapping("/api/webhooks")
public class PlaidWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PlaidWebhookController.class);
    private static final String PLAID_SIGNATURE_HEADER = "Plaid-Verification";

    private final PlaidWebhookService webhookService;

    public PlaidWebhookController(PlaidWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * Plaid webhook receiver.
     * Reads the raw body as a string (required for HMAC verification),
     * validates the signature, then dispatches async processing.
     */
    @PostMapping(value = "/plaid", consumes = "application/json")
    public ResponseEntity<Map<String, String>> handlePlaidWebhook(
            HttpServletRequest request,
            @RequestBody Map<String, Object> payload) {

        // ── Read raw body for signature verification ───────────────────────────
        // Note: Spring has already read the body into `payload`, so we
        // re-serialize it for the HMAC check. For strict production use,
        // configure a RawBodyCachingFilter (see comment below).
        String rawBody = buildRawBody(payload);
        String signature = request.getHeader(PLAID_SIGNATURE_HEADER);

        // ── Signature gate ─────────────────────────────────────────────────────
        if (!webhookService.verifySignature(rawBody, signature)) {
            log.warn("[Webhook] REJECTED — invalid or missing Plaid-Verification header from {}",
                    request.getRemoteAddr());
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid webhook signature"));
        }

        // ── Dispatch async (Plaid expects a fast 200 response) ─────────────────
        log.info("[Webhook] Accepted event: type={}, code={}, item={}",
                payload.get("webhook_type"), payload.get("webhook_code"), payload.get("item_id"));

        webhookService.processWebhookEvent(payload);

        return ResponseEntity.ok(Map.of("status", "accepted"));
    }

    /**
     * Reconstructs a canonical JSON string for HMAC verification.
     * For production, prefer a servlet Filter that caches the raw bytes
     * before Spring parses the body (avoids re-serialization drift).
     */
    private String buildRawBody(Map<String, Object> payload) {
        StringBuilder sb = new StringBuilder("{");
        payload.forEach((key, value) -> {
            sb.append("\"").append(key).append("\":");
            if (value instanceof String s) {
                sb.append("\"").append(s).append("\"");
            } else if (value instanceof Number n) {
                sb.append(n);
            } else if (value instanceof Boolean b) {
                sb.append(b);
            } else {
                sb.append("\"").append(value).append("\"");
            }
            sb.append(",");
        });
        if (sb.length() > 1)
            sb.setLength(sb.length() - 1); // trim trailing comma
        sb.append("}");
        return sb.toString();
    }
}
