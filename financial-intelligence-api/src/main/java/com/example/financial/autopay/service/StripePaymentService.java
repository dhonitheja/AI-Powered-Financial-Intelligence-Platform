package com.example.financial.autopay.service;

import com.example.financial.autopay.model.dto.response.PaymentMethodResponse;
import com.example.financial.autopay.model.entity.AutoPayExecutionLog;
import com.example.financial.autopay.model.entity.AutoPaySchedule;
import com.example.financial.autopay.model.entity.ExecutionStatus;
import com.example.financial.autopay.model.entity.PlaidVerificationStatus;
import com.example.financial.autopay.repository.AutoPayExecutionLogRepository;
import com.example.financial.autopay.repository.AutoPayScheduleRepository;
import com.example.financial.entity.AppUser;
import com.example.financial.repository.AppUserRepository;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.model.SetupIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodListParams;
import com.stripe.param.SetupIntentCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Full Stripe payment execution service for the AutoPay Hub.
 *
 * <p>
 * Safety guarantees on every execution:
 * <ol>
 * <li>Balance guard via Plaid real-time balance.</li>
 * <li>Duplicate guard checks bank transactions for this cycle.</li>
 * <li>Amount tamper-proof: always loaded from DB, never from request.</li>
 * <li>Ownership check: user must own the schedule.</li>
 * <li>All results logged to {@code autopay_execution_logs} regardless of
 * outcome.</li>
 * </ol>
 *
 * <p>
 * Security invariants:
 * <ul>
 * <li>Secret key managed by {@link com.example.financial.config.StripeConfig} —
 * never re-set here.</li>
 * <li>PaymentIntent {@code client_secret} is NEVER logged or returned.</li>
 * <li>Card numbers, CVVs are NEVER processed — Stripe tokenises everything
 * client-side.</li>
 * <li>Error messages are sanitised before storage (digit sequences
 * redacted).</li>
 * </ul>
 */
@Service
public class StripePaymentService {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentService.class);

    /** Stripe error codes mapped to safe, user-facing messages. */
    private static final java.util.Map<String, String> DECLINE_MESSAGES = java.util.Map.of(
            "insufficient_funds", "Payment failed: Insufficient funds",
            "card_declined", "Payment failed: Card declined",
            "expired_card", "Payment failed: Card has expired",
            "incorrect_cvc", "Payment failed: Incorrect security code",
            "lost_card", "Payment failed: Card unavailable",
            "stolen_card", "Payment failed: Card unavailable",
            "do_not_honor", "Payment failed: Card declined by bank",
            "authentication_required", "Payment failed: Card authentication required");

    private final AutoPayScheduleRepository scheduleRepository;
    private final AutoPayExecutionLogRepository executionLogRepository;
    private final BalanceGuardService balanceGuardService;
    private final AppUserRepository userRepository;

    public StripePaymentService(
            AutoPayScheduleRepository scheduleRepository,
            AutoPayExecutionLogRepository executionLogRepository,
            BalanceGuardService balanceGuardService,
            AppUserRepository userRepository) {
        this.scheduleRepository = scheduleRepository;
        this.executionLogRepository = executionLogRepository;
        this.balanceGuardService = balanceGuardService;
        this.userRepository = userRepository;
    }

    // ── 1. Customer Management ─────────────────────────────────────────────────

    /**
     * Returns the user's existing Stripe Customer, or creates one if this is their
     * first time.
     * Uses userId as idempotency key to prevent duplicate customers on concurrent
     * requests.
     *
     * @param userId the application user's UUID string
     * @param email  the user's email address (used only for Stripe record, not
     *               returned)
     * @return the Stripe Customer ID (cus_xxx)
     */
    @Transactional
    public String createOrGetCustomer(String userId, String email) throws StripeException {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Return existing customer ID if already registered
        if (user.getStripeCustomerId() != null && !user.getStripeCustomerId().isBlank()) {
            log.info("[Stripe] Returning existing customer for user {}", userId);
            return user.getStripeCustomerId();
        }

        // Create new Stripe Customer — idempotency key prevents duplicates on retry
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(email)
                .setMetadata(java.util.Map.of("app_user_id", userId))
                .build();

        Customer customer = Customer.create(params,
                com.stripe.net.RequestOptions.builder()
                        .setIdempotencyKey("create-customer-" + userId)
                        .build());

        // Persist the customer ID — safe to store (not sensitive)
        user.setStripeCustomerId(customer.getId());
        userRepository.save(user);

        log.info("[Stripe] Created new Stripe customer for user {}", userId);
        return customer.getId();
    }

    // ── 2. SetupIntent ─────────────────────────────────────────────────────────

    /**
     * Creates a SetupIntent to securely collect card details on the frontend.
     * The returned {@code clientSecret} is sent to the frontend ONLY — it is
     * never logged here.
     *
     * @param stripeCustomerId the Stripe customer ID (cus_xxx)
     * @return the SetupIntent client_secret (seti_xxx_secret_xxx)
     */
    public String createSetupIntent(String stripeCustomerId) throws StripeException {
        SetupIntentCreateParams params = SetupIntentCreateParams.builder()
                .setCustomer(stripeCustomerId)
                .setUsage(SetupIntentCreateParams.Usage.OFF_SESSION)
                .addPaymentMethodType("card")
                .build();

        SetupIntent intent = SetupIntent.create(params);

        // Log ONLY the SetupIntent ID — never the client_secret
        log.info("[Stripe] SetupIntent {} created for customer {}", intent.getId(), stripeCustomerId);

        // Return client_secret — passed to frontend for Stripe.js; never stored
        return intent.getClientSecret();
    }

    // ── 3. Payment Method Management ──────────────────────────────────────────

    /**
     * Attaches a PaymentMethod to the customer and sets it as their default.
     * Called after the frontend confirms a SetupIntent.
     *
     * @param userId          the app user's UUID string (for ownership
     *                        verification)
     * @param paymentMethodId Stripe PaymentMethod ID (pm_xxx)
     * @return masked card details (last4, brand, expiry only)
     */
    @Transactional
    public PaymentMethodResponse attachPaymentMethod(String userId, String paymentMethodId) throws StripeException {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getStripeCustomerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No Stripe customer found. Please create a SetupIntent first.");
        }

        // Attach PM to customer
        PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
        pm.attach(com.stripe.param.PaymentMethodAttachParams.builder()
                .setCustomer(user.getStripeCustomerId())
                .build());

        // Set as default on the Stripe Customer object
        Customer.retrieve(user.getStripeCustomerId()).update(
                CustomerUpdateParams.builder()
                        .setInvoiceSettings(CustomerUpdateParams.InvoiceSettings.builder()
                                .setDefaultPaymentMethod(paymentMethodId)
                                .build())
                        .build());

        // Persist ONLY the PM ID — never raw card data
        user.setStripeDefaultPaymentMethod(paymentMethodId);
        userRepository.save(user);

        log.info("[Stripe] PaymentMethod attached for user {} (PM ID truncated for security)", userId);
        return maskPaymentMethod(pm, true);
    }

    /**
     * Lists all saved payment methods for a customer.
     * Returns ONLY masked data (last4, brand, expiry).
     *
     * @param userId the application user's UUID string
     */
    public List<PaymentMethodResponse> listPaymentMethods(String userId) throws StripeException {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getStripeCustomerId() == null) {
            return java.util.Collections.emptyList();
        }

        PaymentMethodListParams params = PaymentMethodListParams.builder()
                .setCustomer(user.getStripeCustomerId())
                .setType(PaymentMethodListParams.Type.CARD)
                .build();

        PaymentMethodCollection collection = PaymentMethod.list(params);
        String defaultPmId = user.getStripeDefaultPaymentMethod();

        return collection.getData().stream()
                .map(pm -> maskPaymentMethod(pm, pm.getId().equals(defaultPmId)))
                .collect(Collectors.toList());
    }

    /**
     * Detaches (removes) a payment method after verifying the caller owns it.
     *
     * @param userId          the app user's UUID string
     * @param paymentMethodId the PM to remove
     */
    @Transactional
    public void detachPaymentMethod(String userId, String paymentMethodId) throws StripeException {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Ownership check: verify this PM belongs to the user's customer
        PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
        if (user.getStripeCustomerId() == null
                || !user.getStripeCustomerId().equals(pm.getCustomer())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Payment method does not belong to this account");
        }

        pm.detach();

        // Clear default if it was the default PM
        if (paymentMethodId.equals(user.getStripeDefaultPaymentMethod())) {
            user.setStripeDefaultPaymentMethod(null);
            userRepository.save(user);
        }

        log.info("[Stripe] PaymentMethod detached for user {}", userId);
    }

    // ── 4. Payment Execution ──────────────────────────────────────────────────

    /**
     * Executes a payment for an AutoPay schedule:
     * <ol>
     * <li>Load schedule + validate ownership</li>
     * <li>Plaid balance guard (10% buffer)</li>
     * <li>Plaid duplicate guard (bank auto-debit check)</li>
     * <li>Stripe PaymentIntent (off_session, customer PM)</li>
     * <li>Log result with retry_count</li>
     * </ol>
     *
     * @param scheduleId the schedule UUID
     * @param userId     the authenticated user's UUID
     * @return the saved execution log
     */
    @Transactional
    public AutoPayExecutionLog execute(UUID scheduleId, UUID userId) {
        AppUser user = userRepository.findById(userId.toString())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        AutoPaySchedule schedule = scheduleRepository
                .findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Schedule not found or access denied"));

        // Amount is ALWAYS loaded from DB — never from request params (tamper-proof)
        BigDecimal amount = schedule.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Schedule has invalid payment amount");
        }

        AutoPayExecutionLog execLog = AutoPayExecutionLog.pending(schedule, user, amount);

        // ── Guard 1: Plaid balance check ──────────────────────────────────────
        if (!balanceGuardService.hasSufficientBalance(userId.toString(), schedule)) {
            execLog.setStatus(ExecutionStatus.SKIPPED);
            execLog.setFailureReason("Insufficient balance (less than required with 10% buffer)");
            executionLogRepository.save(execLog);
            log.warn("[Stripe] Skipped '{}' — insufficient balance", schedule.getPaymentName());
            return execLog;
        }

        // ── Guard 2: Plaid duplicate check ────────────────────────────────────
        int cycleDays = getCycleDays(schedule);
        if (balanceGuardService.isAlreadyPaidThisCycle(userId.toString(), schedule, cycleDays)) {
            execLog.setStatus(ExecutionStatus.SKIPPED);
            execLog.setFailureReason("Bank auto-debit detected via Plaid — payment already made this cycle");
            executionLogRepository.save(execLog);
            log.info("[Stripe] Skipped '{}' — duplicate payment detected", schedule.getPaymentName());
            return execLog;
        }

        return doExecuteStripePayment(execLog, user, schedule, amount);
    }

    /**
     * Retries a previously failed execution log.
     * Re-runs the balance and duplicate guards, and creates a new Stripe
     * PaymentIntent.
     * The retry count is maintained on the log.
     *
     * @param execLogId the UUID of the failed execution log to retry
     */
    @Transactional
    public AutoPayExecutionLog retryFailedPayment(UUID execLogId) {
        AutoPayExecutionLog execLog = executionLogRepository.findById(execLogId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Execution log not found"));

        if (execLog.getStatus() != ExecutionStatus.FAILED) {
            log.info("[Stripe] Skipping retry for log {}: status is {}", execLogId, execLog.getStatus());
            return execLog;
        }

        AutoPaySchedule schedule = execLog.getSchedule();
        AppUser user = execLog.getUser();
        BigDecimal amount = execLog.getAmountPaid();

        log.info("[Stripe] Retrying payment for schedule '{}' (attempt #{})",
                schedule.getPaymentName(), (execLog.getRetryCount() != null ? execLog.getRetryCount() : 0) + 1);

        execLog.setStatus(ExecutionStatus.PENDING);

        // ── Guard 1: Plaid balance check ──────────────────────────────────────
        if (!balanceGuardService.hasSufficientBalance(user.getId().toString(), schedule)) {
            execLog.setStatus(ExecutionStatus.FAILED);
            execLog.setFailureReason("Retry failed: Insufficient balance (less than required with 10% buffer)");
            executionLogRepository.save(execLog);
            log.warn("[Stripe] Retry skipped '{}' — insufficient balance", schedule.getPaymentName());
            return execLog;
        }

        // ── Guard 2: Plaid duplicate check ────────────────────────────────────
        int cycleDays = getCycleDays(schedule);
        if (balanceGuardService.isAlreadyPaidThisCycle(user.getId().toString(), schedule, cycleDays)) {
            execLog.setStatus(ExecutionStatus.SKIPPED);
            execLog.setFailureReason(
                    "Retry aborted: Bank auto-debit detected via Plaid — payment already made this cycle");
            executionLogRepository.save(execLog);
            log.info("[Stripe] Retry aborted '{}' — duplicate payment detected", schedule.getPaymentName());
            return execLog;
        }

        return doExecuteStripePayment(execLog, user, schedule, amount);
    }

    // ── Internal Execution ────────────────────────────────────────────────────

    private AutoPayExecutionLog doExecuteStripePayment(AutoPayExecutionLog execLog, AppUser user,
            AutoPaySchedule schedule, BigDecimal amount) {

        // ── Guard 3: Stripe customer & payment method must exist ──────────────
        if (user.getStripeCustomerId() == null || user.getStripeDefaultPaymentMethod() == null) {
            execLog.setStatus(ExecutionStatus.FAILED);
            execLog.setFailureReason("No payment method on file. Please add a card to your account.");
            executionLogRepository.save(execLog);
            log.warn("[Stripe] No customer/PM for user {} schedule '{}'",
                    user.getId(), schedule.getPaymentName());
            return execLog;
        }

        // ── Execute PaymentIntent ─────────────────────────────────────────────
        try {
            long amountCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountCents)
                    .setCurrency(schedule.getCurrency().toLowerCase())
                    .setCustomer(user.getStripeCustomerId())
                    .setPaymentMethod(user.getStripeDefaultPaymentMethod())
                    .setConfirm(true)
                    .setOffSession(true)
                    // Metadata for webhook reconciliation — never use for financial decisions
                    .putMetadata("schedule_id", schedule.getId().toString())
                    .putMetadata("user_id", user.getId().toString())
                    .putMetadata("category", schedule.getPaymentCategory().name())
                    .putMetadata("payment_name", schedule.getPaymentName())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            // Store PI ID (safe) — never store client_secret
            execLog.setStripePaymentIntentId(intent.getId());
            execLog.setStatus(resolveStatus(intent.getStatus()));
            execLog.setPlaidVerificationStatus(
                    com.example.financial.autopay.model.entity.PlaidVerificationStatus.UNVERIFIED);
            executionLogRepository.save(execLog);

            // Log ONLY the PI ID and schedule name — no amounts or user data
            log.info("[Stripe] PI {} created for '{}' status={}",
                    intent.getId(), schedule.getPaymentName(), intent.getStatus());

        } catch (com.stripe.exception.CardException e) {
            execLog.setStatus(ExecutionStatus.FAILED);
            execLog.setFailureReason(friendlyDeclineMessage(e.getCode()));
            executionLogRepository.save(execLog);
            // Log ONLY the decline code, not the message (may contain card hints)
            log.warn("[Stripe] Card declined for '{}': code={}", schedule.getPaymentName(), e.getCode());

        } catch (com.stripe.exception.StripeException e) {
            execLog.setStatus(ExecutionStatus.FAILED);
            execLog.setFailureReason("Payment processing error. Please try again later.");
            executionLogRepository.save(execLog);
            log.error("[Stripe] StripeException for '{}': code={}", schedule.getPaymentName(), e.getCode());

        } catch (Exception e) {
            execLog.setStatus(ExecutionStatus.FAILED);
            execLog.setFailureReason("Unexpected error during payment processing.");
            executionLogRepository.save(execLog);
            log.error("[Stripe] Unexpected error for '{}': type={}",
                    schedule.getPaymentName(), e.getClass().getSimpleName());
        }

        return execLog;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private PaymentMethodResponse maskPaymentMethod(PaymentMethod pm, boolean isDefault) {
        var card = pm.getCard();
        return new PaymentMethodResponse(
                pm.getId(),
                card != null ? card.getBrand() : "unknown",
                card != null ? card.getLast4() : "****",
                card != null && card.getExpMonth() != null ? card.getExpMonth().intValue() : 0,
                card != null && card.getExpYear() != null ? card.getExpYear().intValue() : 0,
                isDefault);
    }

    private ExecutionStatus resolveStatus(String stripeStatus) {
        return switch (stripeStatus != null ? stripeStatus : "") {
            case "succeeded" -> ExecutionStatus.SUCCESS;
            case "canceled" -> ExecutionStatus.CANCELLED;
            case "requires_payment_method",
                    "requires_confirmation",
                    "requires_action" ->
                ExecutionStatus.PENDING;
            default -> ExecutionStatus.PENDING;
        };
    }

    private int getCycleDays(AutoPaySchedule schedule) {
        return switch (schedule.getFrequency()) {
            case DAILY -> 1;
            case WEEKLY -> 7;
            case BIWEEKLY -> 14;
            case MONTHLY -> 31;
            case QUARTERLY -> 92;
            case ANNUALLY -> 366;
        };
    }

    /** Maps Stripe decline codes to safe user-facing messages. */
    private String friendlyDeclineMessage(String code) {
        if (code == null)
            return "Payment failed: Card declined";
        return DECLINE_MESSAGES.getOrDefault(code, "Payment failed: Please check your payment method");
    }
}
