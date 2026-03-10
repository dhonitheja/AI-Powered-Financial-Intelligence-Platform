package com.example.financial.autopay.controller;

import com.example.financial.autopay.model.entity.ExecutionStatus;
import com.example.financial.autopay.model.entity.PlaidVerificationStatus;
import com.example.financial.autopay.repository.AutoPayExecutionLogRepository;
import com.example.financial.autopay.repository.AutoPayScheduleRepository;
import com.example.financial.repository.AppUserRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.SetupIntent;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Handles incoming Stripe webhook events.
 *
 * <p>
 * Critical security rules:
 * <ol>
 * <li>This endpoint is EXEMPT from JWT authentication — Stripe cannot
 * authenticate via JWT.</li>
 * <li>Every request MUST pass Stripe signature verification before any
 * processing.
 * An unverified request is treated as an attack: return 400, log
 * minimally.</li>
 * <li>Webhook secret is injected from env — never hard-coded or logged.</li>
 * <li>Idempotent: re-delivery of the same event is handled safely.</li>
 * <li>No sensitive data (client_secret, card numbers) is logged or stored.</li>
 * </ol>
 *
 * <p>
 * To exempt from JWT filter:
 * {@link com.example.financial.security.WebSecurityConfig}
 * permits {@code /api/v1/autopay/stripe/webhook} without authentication.
 */
@RestController
@RequestMapping("/api/v1/autopay/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    private final AutoPayExecutionLogRepository executionLogRepository;
    private final AutoPayScheduleRepository scheduleRepository;
    private final AppUserRepository userRepository;

    public StripeWebhookController(
            AutoPayExecutionLogRepository executionLogRepository,
            AutoPayScheduleRepository scheduleRepository,
            AppUserRepository userRepository) {
        this.executionLogRepository = executionLogRepository;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
    }

    /**
     * Stripe webhook receiver.
     *
     * <p>
     * Stripe requires the raw, unmodified request body for signature verification.
     * Do NOT use {@code @RequestBody String} with any message converters that
     * modify it.
     *
     * <p>
     * Returns 200 quickly — heavy processing should be async in production.
     */
    @PostMapping(value = "/webhook", consumes = "application/json")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        // ── Step 1: ALWAYS verify signature first ─────────────────────────────
        if (sigHeader == null || sigHeader.isBlank()) {
            log.warn("[StripeWebhook] Request missing Stripe-Signature header — rejected");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Missing signature"));
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            // Log ONLY that verification failed — don't expose webhook secret or payload
            log.warn("[StripeWebhook] Signature verification failed — possible spoofed request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Signature verification failed"));
        } catch (Exception e) {
            log.error("[StripeWebhook] Event parsing error: {}", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid event format"));
        }

        // ── Step 2: Route to handler ──────────────────────────────────────────
        log.info("[StripeWebhook] Processing verified event: type={} id={}", event.getType(), event.getId());

        try {
            switch (event.getType()) {
                case "payment_intent.succeeded" -> handlePaymentSucceeded(event);
                case "payment_intent.payment_failed" -> handlePaymentFailed(event);
                case "setup_intent.succeeded" -> handleSetupIntentSucceeded(event);
                case "customer.deleted" -> handleCustomerDeleted(event);
                default -> log.debug("[StripeWebhook] Unhandled event type: {}", event.getType());
            }
        } catch (Exception e) {
            // Log error but return 200 — Stripe will retry on non-200, causing infinite
            // loops
            log.error("[StripeWebhook] Handler error for event {}: {}", event.getId(), e.getMessage());
        }

        // Always return 200 to acknowledge receipt to Stripe
        return ResponseEntity.ok(Map.of("received", "true"));
    }

    // ── Event Handlers ─────────────────────────────────────────────────────────

    /**
     * payment_intent.succeeded:
     * - Update execution log to SUCCESS
     * - Mark as UNVERIFIED (Plaid will confirm later)
     */
    private void handlePaymentSucceeded(Event event) {
        PaymentIntent intent = extractPaymentIntent(event).orElse(null);
        if (intent == null)
            return;

        String piId = intent.getId();
        executionLogRepository.findByStripePaymentIntentId(piId).ifPresent(execLog -> {
            if (execLog.getStatus() == ExecutionStatus.SUCCESS) {
                log.debug("[StripeWebhook] Idempotent: PI {} already SUCCESS", piId);
                return;
            }
            execLog.setStatus(ExecutionStatus.SUCCESS);
            execLog.setPlaidVerificationStatus(PlaidVerificationStatus.UNVERIFIED);
            executionLogRepository.save(execLog);
            log.info("[StripeWebhook] PI {} marked SUCCESS — pending Plaid verification", piId);
        });
    }

    /**
     * payment_intent.payment_failed:
     * - Update execution log to FAILED
     * - Store a user-friendly (not raw Stripe) failure reason
     * - Trigger retry logic if retryCount < 3
     */
    private void handlePaymentFailed(Event event) {
        PaymentIntent intent = extractPaymentIntent(event).orElse(null);
        if (intent == null)
            return;

        String piId = intent.getId();
        String declineCode = extractDeclineCode(intent);
        String userMessage = mapDeclineCode(declineCode);

        executionLogRepository.findByStripePaymentIntentId(piId).ifPresent(execLog -> {
            if (execLog.getStatus() == ExecutionStatus.FAILED) {
                log.debug("[StripeWebhook] Idempotent: PI {} already FAILED", piId);
                return;
            }
            execLog.setStatus(ExecutionStatus.FAILED);
            execLog.setFailureReason(userMessage);

            // Increment retry count for downstream retry scheduler
            int retryCount = execLog.getRetryCount() != null ? execLog.getRetryCount() : 0;
            execLog.setRetryCount(retryCount + 1);

            executionLogRepository.save(execLog);
            log.warn("[StripeWebhook] PI {} FAILED: code={}", piId, declineCode);
        });
    }

    /**
     * setup_intent.succeeded:
     * - Log confirmation that card was saved on Stripe side
     * - No DB update needed here; attachPaymentMethod() handles the DB side
     */
    private void handleSetupIntentSucceeded(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        deserializer.getObject().ifPresent(obj -> {
            if (obj instanceof SetupIntent si) {
                log.info("[StripeWebhook] SetupIntent {} succeeded for customer {}", si.getId(), si.getCustomer());
                // In production: send confirmation push notification / email to user
            }
        });
    }

    /**
     * customer.deleted:
     * - Deactivate ALL autopay schedules for the deleted customer
     * - Users must re-link their payment method
     */
    private void handleCustomerDeleted(Event event) {
        // Extract customer ID directly from the event object
        try {
            event.getDataObjectDeserializer().getObject().ifPresent(obj -> {
                if (obj instanceof com.stripe.model.Customer customer) {
                    String customerId = customer.getId();
                    if (customerId == null || customerId.isBlank())
                        return;

                    // Find the user with this Stripe customer ID and deactivate their schedules
                    userRepository.findByStripeCustomerId(customerId).ifPresent(user -> {
                        int deactivated = scheduleRepository.deactivateAllForUser(user.getId());
                        log.warn("[StripeWebhook] Customer {} deleted: deactivated {} schedules for user {}",
                                customerId, deactivated, user.getId());
                    });
                }
            });
        } catch (Exception e) {
            log.error("[StripeWebhook] Failed to handle customer.deleted: {}", e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Optional<PaymentIntent> extractPaymentIntent(Event event) {
        return event.getDataObjectDeserializer().getObject()
                .filter(obj -> obj instanceof PaymentIntent)
                .map(obj -> (PaymentIntent) obj);
    }

    private String extractDeclineCode(PaymentIntent intent) {
        try {
            if (intent.getLastPaymentError() != null) {
                return intent.getLastPaymentError().getCode();
            }
        } catch (Exception ignored) {
        }
        return "card_declined";
    }

    /**
     * Maps Stripe error codes to user-friendly messages. NEVER returns raw Stripe
     * message.
     */
    private String mapDeclineCode(String code) {
        if (code == null)
            return "Payment failed: Please check your payment method";
        return switch (code) {
            case "insufficient_funds" -> "Payment failed: Insufficient funds";
            case "card_declined" -> "Payment failed: Card declined";
            case "expired_card" -> "Payment failed: Card has expired";
            case "incorrect_cvc" -> "Payment failed: Incorrect security code";
            case "authentication_required" -> "Payment failed: Card authentication required";
            default -> "Payment failed: Please check your payment method";
        };
    }
}
