package com.example.financial.health;

import com.example.financial.autopay.service.WealthixAiClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class AiServiceHealthIndicator implements HealthIndicator {

    private final WealthixAiClient aiClient;

    public AiServiceHealthIndicator(WealthixAiClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public Health health() {
        try {
            boolean available = aiClient.isHealthy();
            if (available) {
                return Health.up()
                        .withDetail("ai-service", "wealthix-ai reachable")
                        .build();
            }
            // Degraded — AI is optional, not critical
            return Health.status("DEGRADED")
                    .withDetail("ai-service", "Unreachable — insights disabled")
                    .build();
        } catch (Exception e) {
            return Health.status("DEGRADED")
                    .withDetail("ai-service", "Unavailable")
                    .build();
        }
    }
}
