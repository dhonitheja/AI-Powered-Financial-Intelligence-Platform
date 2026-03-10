package com.example.financial.autopay.controller;

import com.example.financial.autopay.model.dto.request.CreateAutoPayRequest;
import com.example.financial.autopay.model.dto.request.UpdateAutoPayRequest;
import com.example.financial.autopay.model.dto.response.AutoPayDashboardResponse;
import com.example.financial.autopay.model.dto.response.AutoPayExecutionLogResponse;
import com.example.financial.autopay.model.dto.response.AutoPayScheduleResponse;
import com.example.financial.autopay.model.dto.response.DetectedRecurringPaymentDTO;
import com.example.financial.autopay.model.dto.response.PaymentMethodResponse;
import com.example.financial.autopay.model.entity.AutoPayExecutionLog;
import com.example.financial.autopay.service.AutoPayService;
import com.example.financial.autopay.service.PlaidVerificationService;
import com.example.financial.autopay.service.RecurringPaymentDetectionService;
import com.example.financial.autopay.service.StripePaymentService;
import com.example.financial.config.StripeConfig;
import com.example.financial.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for the AutoPay Hub.
 *
 * <p>
 * Security:
 * <ul>
 * <li>All endpoints are protected with
 * {@code @PreAuthorize("isAuthenticated()")}.
 * <li>The authenticated user's email is resolved from the JWT principal —
 * never from request parameters or body.
 * <li>Ownership validation is enforced in the service layer for every resource
 * operation (prevents IDOR attacks).
 * <li>Rate limiting on write operations is applied via the existing
 * {@link com.example.financial.security.ChatRateLimiterService} pattern;
 * a dedicated AutoPayRateLimiterService should be wired here for production.
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/autopay")
@PreAuthorize("isAuthenticated()")
@Tag(name = "AutoPay Hub", description = "Centralized recurring payment and EMI manager")
public class AutoPayController {

    private final AutoPayService autoPayService;
    private final RecurringPaymentDetectionService detectionService;
    private final StripePaymentService stripePaymentService;
    private final PlaidVerificationService plaidVerificationService;
    private final StripeConfig stripeConfig;

    public AutoPayController(
            AutoPayService autoPayService,
            RecurringPaymentDetectionService detectionService,
            StripePaymentService stripePaymentService,
            PlaidVerificationService plaidVerificationService,
            StripeConfig stripeConfig) {
        this.autoPayService = autoPayService;
        this.detectionService = detectionService;
        this.stripePaymentService = stripePaymentService;
        this.plaidVerificationService = plaidVerificationService;
        this.stripeConfig = stripeConfig;
    }

    // ── Schedules ─────────────────────────────────────────────────────────────

    @PostMapping("/schedules")
    @Operation(summary = "Create a new autopay schedule")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AutoPayScheduleResponse> createSchedule(
            @Valid @RequestBody CreateAutoPayRequest request) {
        String email = resolveEmail();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(autoPayService.create(email, request));
    }

    @GetMapping("/schedules")
    @Operation(summary = "Get all active autopay schedules (paginated)")
    public ResponseEntity<Page<AutoPayScheduleResponse>> listSchedules(
            @PageableDefault(size = 20, sort = "nextDueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(autoPayService.listSchedules(resolveEmail(), pageable));
    }

    @GetMapping("/schedules/{id}")
    @Operation(summary = "Get a single autopay schedule by ID")
    public ResponseEntity<AutoPayScheduleResponse> getSchedule(
            @PathVariable("id") UUID id) {
        return ResponseEntity.ok(autoPayService.getById(resolveEmail(), id));
    }

    @PutMapping("/schedules/{id}")
    @Operation(summary = "Update an autopay schedule")
    public ResponseEntity<AutoPayScheduleResponse> updateSchedule(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAutoPayRequest request) {
        return ResponseEntity.ok(autoPayService.update(resolveEmail(), id, request));
    }

    @DeleteMapping("/schedules/{id}")
    @Operation(summary = "Soft-delete an autopay schedule (sets is_active=false)")
    public ResponseEntity<Map<String, String>> deleteSchedule(
            @PathVariable("id") UUID id) {
        autoPayService.softDelete(resolveEmail(), id);
        return ResponseEntity.ok(Map.of("message", "Schedule deactivated successfully"));
    }

    @PatchMapping("/schedules/{id}/toggle")
    @Operation(summary = "Toggle active/inactive status of an autopay schedule")
    public ResponseEntity<AutoPayScheduleResponse> toggleSchedule(
            @PathVariable("id") UUID id) {
        return ResponseEntity.ok(autoPayService.toggleActive(resolveEmail(), id));
    }

    @GetMapping("/schedules/upcoming")
    @Operation(summary = "Get payments due in the next N days (default 30)")
    public ResponseEntity<List<AutoPayScheduleResponse>> getUpcoming(
            @RequestParam(name = "days", defaultValue = "30") int days) {
        if (days < 1 || days > 365) {
            days = 30; // sanitise to safe bounds
        }
        return ResponseEntity.ok(autoPayService.getUpcoming(resolveEmail(), days));
    }

    @GetMapping("/schedules/overdue")
    @Operation(summary = "Get overdue autopay schedules")
    public ResponseEntity<List<AutoPayScheduleResponse>> getOverdue() {
        return ResponseEntity.ok(autoPayService.getOverdue(resolveEmail()));
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @Operation(summary = "Get AutoPay dashboard: obligations, health score, upcoming dues")
    public ResponseEntity<AutoPayDashboardResponse> getDashboard() {
        return ResponseEntity.ok(autoPayService.getDashboard(resolveEmail()));
    }

    /**
     * Manually trigger payment execution for a schedule.
     * Runs: balance guard → duplicate guard → Stripe PaymentIntent.
     */
    @PostMapping("/schedules/{id}/execute")
    @Operation(summary = "Manually trigger Stripe payment execution for a schedule")
    public ResponseEntity<AutoPayExecutionLogResponse> executePayment(
            @PathVariable("id") UUID id) {
        String email = resolveEmail();
        UUID userId = autoPayService.resolveUserId(email);
        AutoPayExecutionLog log = stripePaymentService.execute(id, userId);
        return ResponseEntity.ok(autoPayService.toExecutionLogResponse(log));
    }

    // ── Execution Logs ────────────────────────────────────────────────────────

    @GetMapping("/execution-logs")
    @Operation(summary = "Get paginated execution history for the authenticated user")
    public ResponseEntity<Page<AutoPayExecutionLogResponse>> getExecutionLogs(
            @PageableDefault(size = 20, sort = "executionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(autoPayService.getExecutionLogs(resolveEmail(), pageable));
    }

    @GetMapping("/execution-logs/{scheduleId}")
    @Operation(summary = "Get execution logs for a specific schedule")
    public ResponseEntity<Page<AutoPayExecutionLogResponse>> getExecutionLogsForSchedule(
            @PathVariable("scheduleId") UUID scheduleId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                autoPayService.getExecutionLogsForSchedule(resolveEmail(), scheduleId, pageable));
    }

    // ── Plaid Intelligence ────────────────────────────────────────────────────

    /**
     * Detects recurring payments from the user's Plaid transaction history.
     * Returns suggestions only — nothing is saved until the user confirms.
     *
     * <p>
     * Security: Returns only anonymised data (descriptions, amounts, categories).
     * No account numbers or Plaid transaction IDs are returned.
     */
    @GetMapping("/detect-recurring")
    @Operation(summary = "Detect recurring payments from Plaid transaction history")
    public ResponseEntity<List<DetectedRecurringPaymentDTO>> detectRecurring() {
        UUID userId = autoPayService.resolveUserId(resolveEmail());
        return ResponseEntity.ok(detectionService.detectRecurring(userId.toString()));
    }

    /**
     * Triggers on-demand Plaid verification of pending execution logs.
     * Called automatically after a Plaid sync but can also be triggered manually.
     */
    @PostMapping("/verify-payments")
    @Operation(summary = "Run on-demand Plaid verification of recent executions")
    public ResponseEntity<Map<String, String>> verifyPayments() {
        UUID userId = autoPayService.resolveUserId(resolveEmail());
        plaidVerificationService.verifyForUser(userId);
        return ResponseEntity.ok(Map.of("message", "Verification triggered successfully"));
    }

    // ── Category Metadata ─────────────────────────────────────────────────────

    @GetMapping("/categories")
    @Operation(summary = "List all supported payment categories with display metadata")
    public ResponseEntity<List<Map<String, Object>>> getCategories() {
        return ResponseEntity.ok(autoPayService.getCategoryMetadata());
    }

    // ── Stripe Execution Layer ────────────────────────────────────────────────

    @PostMapping("/stripe/setup-intent")
    @Operation(summary = "Create a SetupIntent for the frontend to collect card details securely")
    public ResponseEntity<Map<String, String>> createSetupIntent() throws com.stripe.exception.StripeException {
        UUID userId = autoPayService.resolveUserId(resolveEmail());
        String customerId = stripePaymentService.createOrGetCustomer(userId.toString(), resolveEmail());
        String clientSecret = stripePaymentService.createSetupIntent(customerId);

        // We only return the SetupIntent secret — actual card tokenization happens
        // securely
        // on the frontend via Stripe.js.
        // We also need to send the publishable key to initialize Stripe.js.
        return ResponseEntity.ok(Map.of(
                "clientSecret", clientSecret,
                "publishableKey", stripeConfig.getPublishableKey()));
    }

    @PostMapping("/stripe/payment-methods")
    @Operation(summary = "Attach a confirmed PaymentMethod to the user")
    public ResponseEntity<PaymentMethodResponse> attachPaymentMethod(
            @RequestBody Map<String, String> body) throws com.stripe.exception.StripeException {
        UUID userId = autoPayService.resolveUserId(resolveEmail());
        String paymentMethodId = body.get("paymentMethodId");

        if (paymentMethodId == null || paymentMethodId.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "paymentMethodId is required");
        }

        return ResponseEntity.ok(stripePaymentService.attachPaymentMethod(userId.toString(), paymentMethodId));
    }

    @GetMapping("/stripe/payment-methods")
    @Operation(summary = "List user's saved payment methods (masked)")
    public ResponseEntity<List<PaymentMethodResponse>> listPaymentMethods()
            throws com.stripe.exception.StripeException {
        UUID userId = autoPayService.resolveUserId(resolveEmail());
        return ResponseEntity.ok(stripePaymentService.listPaymentMethods(userId.toString()));
    }

    @DeleteMapping("/stripe/payment-methods/{paymentMethodId}")
    @Operation(summary = "Detach a payment method from the user's account")
    public ResponseEntity<Map<String, String>> detachPaymentMethod(
            @PathVariable("paymentMethodId") String paymentMethodId) throws com.stripe.exception.StripeException {
        UUID userId = autoPayService.resolveUserId(resolveEmail());
        stripePaymentService.detachPaymentMethod(userId.toString(), paymentMethodId);
        return ResponseEntity.ok(Map.of("message", "Payment method removed successfully"));
    }

    // ── Security helpers ──────────────────────────────────────────────────────

    /**
     * Resolves the authenticated user's email from the JWT principal.
     * NEVER read user identity from request body or query params.
     */
    private String resolveEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getEmail();
        }
        throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Not authenticated");
    }
}
