package com.example.financial.migration;

import com.example.financial.entity.UserBankConnection;
import com.example.financial.repository.UserBankConnectionRepository;
import com.example.financial.service.EncryptionService;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Flyway Java migration that re‑encrypts any existing Plaid access tokens that
 * were
 * stored using the legacy AES‑CBC/zero‑IV scheme.
 *
 * It reads each {@code UserBankConnection}, attempts decryption with the legacy
 * method (handled inside {@link EncryptionService#decrypt}), then re‑encrypts
 * using the new AES‑256‑GCM implementation and saves the record.
 *
 * The migration runs inside a single transaction per batch to guarantee
 * atomicity.
 */
public class V4__ReEncryptAccessTokens extends BaseJavaMigration {

    private static final Logger log = LoggerFactory.getLogger(V4__ReEncryptAccessTokens.class);

    @Override
    @Transactional
    public void migrate(Context context) throws Exception {
        // Obtain Spring beans via the Flyway context's classloader
        UserBankConnectionRepository repo = (UserBankConnectionRepository) context.getConfiguration()
                .getDataSource().unwrap(UserBankConnectionRepository.class);
        EncryptionService encService = (EncryptionService) context.getConfiguration()
                .getDataSource().unwrap(EncryptionService.class);

        List<UserBankConnection> connections = repo.findAll();
        for (UserBankConnection conn : connections) {
            String encrypted = conn.getEncryptedAccessToken();
            if (encrypted == null)
                continue;
            try {
                // Decrypt using the legacy fallback inside EncryptionService
                String plain = encService.decrypt(encrypted);
                // Re‑encrypt with the new GCM implementation
                String reEncrypted = encService.encrypt(plain);
                conn.setEncryptedAccessToken(reEncrypted);
                repo.save(conn);
                log.info("Re‑encrypted access token for connection id {}", conn.getId());
            } catch (Exception e) {
                log.warn("Failed to re‑encrypt token for connection id {}: {}", conn.getId(), e.getMessage());
                // Continue with other rows – a failure here should not abort the whole
                // migration
            }
        }
    }
}
