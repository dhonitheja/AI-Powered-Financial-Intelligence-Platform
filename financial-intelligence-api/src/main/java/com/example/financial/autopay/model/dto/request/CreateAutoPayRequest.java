package com.example.financial.autopay.model.dto.request;

import com.example.financial.autopay.model.entity.PaymentCategory;
import com.example.financial.autopay.model.entity.PaymentFrequency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for creating a new autopay schedule.
 *
 * Security note: accountNumber and routingNumber are received as plaintext
 * from the client over HTTPS, then immediately encrypted by the service layer
 * before any persistence. They must NEVER be logged.
 */
public class CreateAutoPayRequest {

    @NotBlank(message = "Payment name is required")
    @Size(max = 255, message = "Payment name must be 255 characters or fewer")
    private String paymentName;

    @NotNull(message = "Payment category is required")
    private PaymentCategory paymentCategory;

    @Size(max = 255, message = "Payment provider must be 255 characters or fewer")
    private String paymentProvider;

    /** Plaintext account number — encrypted before persistence. NEVER log. */
    @Size(max = 50, message = "Account number too long")
    @Pattern(regexp = "^[0-9]{4,17}$", message = "Account number must be 4-17 digits")
    private String accountNumber;

    /** Plaintext routing number — encrypted before persistence. NEVER log. */
    @Size(max = 9, message = "Routing number must be at most 9 characters")
    @Pattern(regexp = "^[0-9]{9}$", message = "Routing number must be exactly 9 digits")
    private String routingNumber;

    /** Plaintext user notes — encrypted before persistence. */
    @Size(max = 1000, message = "Notes must be 1000 characters or fewer")
    private String notes;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Digits(integer = 13, fraction = 2, message = "Amount format invalid")
    private BigDecimal amount;

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    private String currency = "USD";

    @NotNull(message = "Frequency is required")
    private PaymentFrequency frequency;

    @NotNull(message = "Next due date is required")
    @FutureOrPresent(message = "Next due date must be today or in the future")
    private LocalDate nextDueDate;

    @Min(value = 1, message = "Due day of month must be between 1 and 31")
    @Max(value = 31, message = "Due day of month must be between 1 and 31")
    private Integer dueDayOfMonth;

    private boolean autoExecute = false;

    @Min(value = 0, message = "Reminder days must be non-negative")
    @Max(value = 30, message = "Reminder days must be at most 30")
    private int reminderDaysBefore = 3;

    // ── Getters & Setters ─────────────────────────────────────────────────────

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

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getRoutingNumber() {
        return routingNumber;
    }

    public void setRoutingNumber(String routingNumber) {
        this.routingNumber = routingNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
}
