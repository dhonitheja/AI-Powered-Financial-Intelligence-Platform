package com.example.financial.autopay.service;

import com.example.financial.autopay.model.entity.AutoPayExecutionLog;
import com.example.financial.autopay.repository.AutoPayExecutionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service orchestrating the retry logic for failed AutoPay executions.
 */
@Service
public class PaymentRetryService {

    private static final Logger log = LoggerFactory.getLogger(PaymentRetryService.class);

    private final AutoPayExecutionLogRepository executionLogRepository;
    private final StripePaymentService stripePaymentService;

    @Value("${autopay.retry.max-attempts:3}")
    private int maxRetries;

    @Value("${autopay.retry.delay-hours:24}")
    private int retryDelayHours;

    public PaymentRetryService(
            AutoPayExecutionLogRepository executionLogRepository,
            StripePaymentService stripePaymentService) {
        this.executionLogRepository = executionLogRepository;
        this.stripePaymentService = stripePaymentService;
    }

    /**
     * Processes eligible retries by finding failed logs older than retryDelayHours
     * and re-attempting via StripePaymentService.
     */
    public void processRetries() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(retryDelayHours);

        List<AutoPayExecutionLog> eligibleLogs = executionLogRepository.findEligibleForRetry(maxRetries, cutoff);

        if (eligibleLogs.isEmpty()) {
            log.info("[PaymentRetry] No eligible failed payments found for retry.");
            return;
        }

        log.info("[PaymentRetry] Found {} payments eligible for retry.", eligibleLogs.size());

        for (AutoPayExecutionLog logEntry : eligibleLogs) {
            try {
                // Execute the retry process
                stripePaymentService.retryFailedPayment(logEntry.getId());
                log.info("[PaymentRetry] Successfully processed retry logic for log {}", logEntry.getId());
            } catch (Exception e) {
                // Catch any unexpected exceptions to prevent halting the entire batch
                log.error("[PaymentRetry] Error processing retry for log {}: {}", logEntry.getId(), e.getMessage());
            }
        }
    }
}
