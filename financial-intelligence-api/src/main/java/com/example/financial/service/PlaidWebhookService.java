package com.example.financial.service;

import com.example.financial.entity.UserBankConnection;
import com.example.financial.repository.UserBankConnectionRepository;
import com.wealthix.plaid.service.PlaidService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles incoming Plaid webhook events:
 * 1. Verifies the Plaid-Verification HMAC-SHA256 signature.
 * 2. Dispatches an async transaction sync for the affected item.
 * 3. Maintains per-user isolation by looking up userId via itemId.
 */
@Service
public class PlaidWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PlaidWebhookService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${plaid.webhook-secret}")
    private String webhookSecret;

    private final PlaidService plaidService;
    private final UserBankConnectionRepository connectionRepository;

    public PlaidWebhookService(PlaidService plaidService,
            UserBankConnectionRepository connectionRepository) {
        this.plaidService = plaidService;
        this.connectionRepository = connectionRepository;
    }

    // ─── Signature Verification ────────────────────────────────────────────────

    /**
     * Verifies the Plaid-Verification header against HMAC-SHA256(webhookSecret,
     * rawBody).
     * Returns true only if the signature matches exactly.
     */
    public boolean verifySignature(String rawBody, String plaidSignatureHeader) {
        if (plaidSignatureHeader == null || plaidSignatureHeader.isBlank()) {
            log.warn("[Webhook] Missing Plaid-Verification header — rejecting request");
            return false;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] computed = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);

            boolean match = computedHex.equalsIgnoreCase(plaidSignatureHeader.trim());
            if (!match) {
                log.warn("[Webhook] Signature mismatch — possible spoofed request");
            }
            return match;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("[Webhook] Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    // ─── Event Dispatch ────────────────────────────────────────────────────────

    /**
     * Processes a verified Plaid webhook event.
     * Supported webhook types:
     * TRANSACTIONS → DEFAULT_UPDATE : new transactions available
     * TRANSACTIONS → HISTORICAL_UPDATE: historical backfill complete
     * ITEM → ERROR : access token revoked / expired
     */
    @Async
    public void processWebhookEvent(Map<String, Object> payload) {
        String webhookType = String.valueOf(payload.getOrDefault("webhook_type", ""));
        String webhookCode = String.valueOf(payload.getOrDefault("webhook_code", ""));
        String itemId = String.valueOf(payload.getOrDefault("item_id", ""));

        log.info("[Webhook] Received {} / {} for item {}", webhookType, webhookCode, itemId);

        switch (webhookType.toUpperCase()) {
            case "TRANSACTIONS" -> handleTransactionEvent(webhookCode, itemId);
            case "ITEM" -> handleItemEvent(webhookCode, itemId);
            default -> log.debug("[Webhook] Unhandled type: {}", webhookType);
        }
    }

    // ─── Event Handlers ────────────────────────────────────────────────────────

    private void handleTransactionEvent(String code, String itemId) {
        switch (code.toUpperCase()) {
            case "DEFAULT_UPDATE", "HISTORICAL_UPDATE", "INITIAL_UPDATE" -> {
                log.info("[Webhook] Transaction update for item {} — triggering sync", itemId);
                triggerSyncForItem(itemId);
            }
            default -> log.debug("[Webhook] Unhandled transaction code: {}", code);
        }
    }

    private void handleItemEvent(String code, String itemId) {
        if ("ERROR".equalsIgnoreCase(code)) {
            log.error("[Webhook] ITEM ERROR for item {} — access token may be revoked", itemId);
            // Future: mark connection as invalid, notify user via email
        }
    }

    // ─── Sync Dispatch ─────────────────────────────────────────────────────────

    /**
     * Finds all users with a connection for this itemId and triggers a Plaid sync
     * for each. This ensures strict user isolation — we only process data for
     * users who actually own this item.
     */
    private void triggerSyncForItem(String itemId) {
        List<UserBankConnection> connections = connectionRepository.findByItemId(itemId);
        if (connections.isEmpty()) {
            log.warn("[Webhook] No connections found for item {} — orphan event ignored", itemId);
            return;
        }

        // Collect unique user IDs to avoid duplicate syncs
        connections.stream()
                .map(UserBankConnection::getUserId)
                .distinct()
                .forEach(userId -> {
                    try {
                        log.info("[Webhook] Triggering sync for user {} (item {})", userId, itemId);
                        plaidService.syncTransactions(userId.toString(), null);
                        log.info("[Webhook] Sync complete for user {} ", userId);
                    } catch (Exception e) {
                        log.error("[Webhook] Sync failed for user {}: {}", userId, e.getMessage());
                    }
                });
    }
}
