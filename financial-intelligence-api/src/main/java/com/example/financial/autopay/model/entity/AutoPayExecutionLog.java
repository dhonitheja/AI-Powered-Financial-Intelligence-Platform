package com.example.financial.autopay.model.entity;

import com.example.financial.entity.AppUser;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable audit record of each payment execution attempt.
 *
 * <p>
 * Security note: {@code failureReason} MUST contain only generic messages
 * (e.g. "Insufficient funds"). NEVER log account numbers, routing numbers,
 * or any PII in this table.
 */
@Entity
@Table(name = "autopay_execution_logs")
public class AutoPayExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private AutoPaySchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "execution_date", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime executionDate;

    @Column(name = "amount_paid", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountPaid;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "execution_status")
    private ExecutionStatus status = ExecutionStatus.PENDING;

    /** Generic failure message — NO PII, NO sensitive financial identifiers. */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /** Stripe PaymentIntent ID — set when payment is executed via Stripe. */
    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId;

    /**
     * The Plaid transaction ID that auto-debited this payment (not set by us —
     * populated post-execution when Plaid sync detects a matching transaction).
     */
    @Column(name = "plaid_transaction_id", length = 255)
    private String plaidTransactionId;

    /**
     * Plaid-based payment verification status.
     * UNVERIFIED → Stripe executed but Plaid hasn't confirmed yet.
     * VERIFIED → Plaid found a matching bank transaction.
     * NEEDS_REVIEW → Not found in Plaid within 5 days.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "plaid_verification_status", length = 20, nullable = false)
    private PlaidVerificationStatus plaidVerificationStatus = PlaidVerificationStatus.UNVERIFIED;

    /** Plaid transaction ID of the bank-confirmed matching transaction. */
    @Column(name = "plaid_matched_transaction_id", length = 255)
    private String plaidMatchedTransactionId;

    /**
     * Number of times Stripe has attempted this payment.
     * Incremented by
     * {@link com.example.financial.autopay.controller.StripeWebhookController}
     * on each {@code payment_intent.payment_failed} event. Max 3 before giving up.
     */
    @Column(name = "retry_count", nullable = false, columnDefinition = "integer default 0")
    private Integer retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    protected AutoPayExecutionLog() {
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    public static AutoPayExecutionLog pending(AutoPaySchedule schedule, AppUser user, BigDecimal amount) {
        AutoPayExecutionLog log = new AutoPayExecutionLog();
        log.schedule = schedule;
        log.user = user;
        log.amountPaid = amount;
        log.executionDate = OffsetDateTime.now();
        log.status = ExecutionStatus.PENDING;
        return log;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public AutoPaySchedule getSchedule() {
        return schedule;
    }

    public AppUser getUser() {
        return user;
    }

    public OffsetDateTime getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(OffsetDateTime executionDate) {
        this.executionDate = executionDate;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public String getPlaidTransactionId() {
        return plaidTransactionId;
    }

    public void setPlaidTransactionId(String plaidTransactionId) {
        this.plaidTransactionId = plaidTransactionId;
    }

    public PlaidVerificationStatus getPlaidVerificationStatus() {
        return plaidVerificationStatus;
    }

    public void setPlaidVerificationStatus(PlaidVerificationStatus plaidVerificationStatus) {
        this.plaidVerificationStatus = plaidVerificationStatus;
    }

    public String getPlaidMatchedTransactionId() {
        return plaidMatchedTransactionId;
    }

    public void setPlaidMatchedTransactionId(String plaidMatchedTransactionId) {
        this.plaidMatchedTransactionId = plaidMatchedTransactionId;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
