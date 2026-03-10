package com.example.financial.autopay.scheduler;

import com.example.financial.autopay.service.PlaidVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly job that cross-references Stripe-executed AutoPay payments against
 * Plaid transaction data to verify actual bank settlement.
 *
 * <p>
 * Schedule: nightly at 2 AM (configurable via AUTOPAY_VERIFICATION_CRON).
 * <p>
 * Security: No PII, amounts, or account data is logged here — only log IDs.
 */
@Component
public class PlaidVerificationJob {

    private static final Logger log = LoggerFactory.getLogger(PlaidVerificationJob.class);

    private final PlaidVerificationService verificationService;

    @Value("${autopay.plaid-verification.review-threshold-days:5}")
    private int reviewThresholdDays;

    public PlaidVerificationJob(PlaidVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @Scheduled(cron = "${autopay.plaid-verification.cron:0 0 2 * * ?}")
    public void run() {
        log.info("[PlaidVerificationJob] Starting nightly verification run");
        try {
            verificationService.runNightlyVerification(reviewThresholdDays);
            log.info("[PlaidVerificationJob] Nightly verification complete");
        } catch (Exception e) {
            log.error("[PlaidVerificationJob] Run failed: {}", e.getMessage());
        }
    }
}
