package com.example.financial.health;

import com.example.financial.service.EncryptionService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class EncryptionHealthIndicator implements HealthIndicator {

    private final EncryptionService encryptionService;

    public EncryptionHealthIndicator(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public Health health() {
        try {
            // Verify encryption service is working
            String testValue = "wealthix-health-check";
            String encrypted = encryptionService.encrypt(testValue);
            String decrypted = encryptionService.decrypt(encrypted);
            if (testValue.equals(decrypted)) {
                return Health.up()
                        .withDetail("encryption", "AES-256-GCM operational")
                        .build();
            }
            return Health.down()
                    .withDetail("encryption", "Decrypt mismatch")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("encryption", "Service unavailable")
                    .build();
        }
    }
}
