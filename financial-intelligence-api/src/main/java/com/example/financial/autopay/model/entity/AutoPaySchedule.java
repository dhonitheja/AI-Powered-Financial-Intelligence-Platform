package com.example.financial.autopay.model.entity;

import com.example.financial.entity.AppUser;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Persisted autopay schedule.
 *
 * <p>
 * Security invariants:
 * <ul>
 * <li>account_number and routing_number are ALWAYS stored AES-256-GCM
 * encrypted via {@link com.example.financial.service.EncryptionService}.
 * <li>Getters/setters for encrypted fields are intentionally named with
 * "Encrypted" suffix to make accidental plaintext assignment obvious
 * in code review.
 * <li>This entity is NEVER serialised directly to HTTP responses — always
 * mapped to
 * {@link com.example.financial.autopay.model.dto.response.AutoPayScheduleResponse}
 * which masks account numbers.
 * </ul>
 */
@Entity
@Table(name = "autopay_schedules", uniqueConstraints = @UniqueConstraint(name = "uq_user_payment_provider", columnNames = {
        "user_id", "payment_name", "payment_provider" }))
public class AutoPaySchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "payment_name", nullable = false, length = 255)
    private String paymentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_category", nullable = false, columnDefinition = "payment_category")
    private PaymentCategory paymentCategory;

    @Column(name = "payment_provider", length = 255)
    private String paymentProvider;

    // ── Encrypted sensitive fields ────────────────────────────────────────────
    /** AES-256-GCM encrypted. Use EncryptionService to read/write. */
    @Column(name = "account_number_encrypted", columnDefinition = "TEXT")
    private String accountNumberEncrypted;

    /** AES-256-GCM encrypted. Use EncryptionService to read/write. */
    @Column(name = "routing_number_encrypted", columnDefinition = "TEXT")
    private String routingNumberEncrypted;

    /** AES-256-GCM encrypted. Use EncryptionService to read/write. */
    @Column(name = "notes_encrypted", columnDefinition = "TEXT")
    private String notesEncrypted;

    // ── Payment details ───────────────────────────────────────────────────────
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, columnDefinition = "payment_frequency")
    private PaymentFrequency frequency;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    @Column(name = "due_day_of_month")
    private Integer dueDayOfMonth;

    // ── Automation & reminders ────────────────────────────────────────────────
    @Column(name = "auto_execute", nullable = false)
    private boolean autoExecute = false;

    @Column(name = "reminder_days_before", nullable = false)
    private int reminderDaysBefore = 3;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // ── Audit ─────────────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime updatedAt;

    // ── Constructors ──────────────────────────────────────────────────────────
    public AutoPaySchedule() {
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public String getPaymentName() {
        return paymentName;
    }

    public void setPaymentName(String paymentName) {
        this.paymentName = paymentName;
    }

    public PaymentCategory getPaymentCategory() {
        return paymentCategory;
    }

    public void setPaymentCategory(PaymentCategory paymentCategory) {
        this.paymentCategory = paymentCategory;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public String getAccountNumberEncrypted() {
        return accountNumberEncrypted;
    }

    public void setAccountNumberEncrypted(String accountNumberEncrypted) {
        this.accountNumberEncrypted = accountNumberEncrypted;
    }

    public String getRoutingNumberEncrypted() {
        return routingNumberEncrypted;
    }

    public void setRoutingNumberEncrypted(String routingNumberEncrypted) {
        this.routingNumberEncrypted = routingNumberEncrypted;
    }

    public String getNotesEncrypted() {
        return notesEncrypted;
    }

    public void setNotesEncrypted(String notesEncrypted) {
        this.notesEncrypted = notesEncrypted;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(PaymentFrequency frequency) {
        this.frequency = frequency;
    }

    public LocalDate getNextDueDate() {
        return nextDueDate;
    }

    public void setNextDueDate(LocalDate nextDueDate) {
        this.nextDueDate = nextDueDate;
    }

    public Integer getDueDayOfMonth() {
        return dueDayOfMonth;
    }

    public void setDueDayOfMonth(Integer dueDayOfMonth) {
        this.dueDayOfMonth = dueDayOfMonth;
    }

    public boolean isAutoExecute() {
        return autoExecute;
    }

    public void setAutoExecute(boolean autoExecute) {
        this.autoExecute = autoExecute;
    }

    public int getReminderDaysBefore() {
        return reminderDaysBefore;
    }

    public void setReminderDaysBefore(int reminderDaysBefore) {
        this.reminderDaysBefore = reminderDaysBefore;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
