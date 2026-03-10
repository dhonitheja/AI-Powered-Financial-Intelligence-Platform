package com.example.financial.autopay.scheduler;

import com.example.financial.autopay.service.PaymentRetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to re-attempt failed AutoPay executions.
 * Triggered via CRON independently from daily reminders or execution triggers.
 */
@Component
public class PaymentRetryJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentRetryJob.class);

    private final PaymentRetryService paymentRetryService;

    public PaymentRetryJob(PaymentRetryService paymentRetryService) {
        this.paymentRetryService = paymentRetryService;
    }

    /**
     * Executes daily retries.
     * Default CRON: 4 AM daily. Overridable via autopay.retry.cron
     */
    @Scheduled(cron = "${autopay.retry.cron:0 0 4 * * ?}")
    public void runFailedPaymentRetries() {
        log.info("[PaymentRetryJob] Starting batch processing for failed payment retries...");
        try {
            paymentRetryService.processRetries();
            log.info("[PaymentRetryJob] Completed batch processing.");
        } catch (Exception e) {
            log.error("[PaymentRetryJob] Error occurred during batch process: {}", e.getMessage(), e);
        }
    }
}
