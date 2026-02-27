package com.example.financial.service;

import com.example.financial.entity.UserBankConnection;
import com.example.financial.repository.UserBankConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * BankSyncService — orchestrates Plaid transaction syncs.
 *
 * Design decisions:
 * - All sync operations derive the user from stored UserBankConnection rows,
 * never from request parameters. userId is never exposed externally.
 * - Access tokens are decrypted only during the sync window and never logged.
 * - Scheduled sync is commented out and replaced by webhook-driven sync.
 * - Async execution prevents blocking the HTTP thread during Plaid API calls.
 */
@Service
public class BankSyncService {

        private static final Logger log = LoggerFactory.getLogger(BankSyncService.class);

        private final PlaidService plaidService;
        private final NotificationService notificationService;
        private final UserBankConnectionRepository connectionRepository;

        public BankSyncService(PlaidService plaidService,
                        NotificationService notificationService,
                        UserBankConnectionRepository connectionRepository) {
                this.plaidService = plaidService;
                this.notificationService = notificationService;
                this.connectionRepository = connectionRepository;
        }

        // ─── Webhook-triggered sync (primary path) ─────────────────────────────────

        /**
         * Called by PlaidWebhookService when a TRANSACTIONS event arrives.
         * User isolation: we only sync the specific user associated with the itemId.
         *
         * @param userId The resolved user ID from the connection record (never from
         *               HTTP payload)
         */
        @Async
        public void syncForUser(String userId) {
                log.info("[BankSync] Starting sync for user {} (webhook-triggered)", userId);
                try {
                        int imported = plaidService.syncTransactions(userId);
                        log.info("[BankSync] Sync complete for user {} — {} new transactions", userId, imported);
                        if (imported > 0) {
                                notificationService.sendBankSyncCompletion(userId, imported);
                        }
                } catch (Exception e) {
                        log.error("[BankSync] Sync failed for user {}: {}", userId, e.getMessage());
                }
        }

        // ─── On-demand sync (called by API controller) ─────────────────────────────

        /**
         * Performs a synchronous Plaid sync for a given user and returns the count.
         * Used by the manual sync endpoint (/api/plaid/sync).
         */
        public int syncNow(String userId) throws Exception {
                log.info("[BankSync] On-demand sync for user {}", userId);
                int imported = plaidService.syncTransactions(userId);
                if (imported > 0) {
                        notificationService.sendBankSyncCompletion(userId, imported);
                }
                return imported;
        }

        // ─── Sync Status Endpoint Support ─────────────────────────────────────────

        /**
         * Returns metadata about a user's linked connections (count, institution
         * names).
         * Used by the frontend polling endpoint to detect connection changes
         * without needing to expose userId on the client.
         */
        public SyncStatusDTO getSyncStatus(String userId) {
                List<UserBankConnection> connections = connectionRepository.findByUserId(userId);
                int accountCount = connections.size();
                long lastUpdatedEpoch = connections.stream()
                                .mapToLong(c -> c.getUpdatedAt() != null
                                                ? c.getUpdatedAt().toEpochSecond(java.time.ZoneOffset.UTC)
                                                : 0L)
                                .max()
                                .orElse(0L);

                return new SyncStatusDTO(accountCount, lastUpdatedEpoch);
        }

        // ─── SyncStatusDTO (inner class, lightweight) ───────────────────────────────

        public record SyncStatusDTO(int accountCount, long lastSyncedAt) {
        }
}
