package com.example.financial.autopay.model.dto.request;

import com.example.financial.autopay.model.entity.PaymentFrequency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for updating an existing autopay schedule.
 * All fields are optional — only non-null values are applied (PATCH-style logic
 * handled in the service layer).
 */
public class UpdateAutoPayRequest {

    @Size(max = 255)
    private String paymentName;

    @Size(max = 255)
    private String paymentProvider;

    /** Plaintext account number — encrypted before persistence. NEVER log. */
    @Size(max = 50)
    @Pattern(regexp = "^[0-9]{4,17}$", message = "Account number must be 4-17 digits")
    private String accountNumber;

    /** Plaintext routing number — encrypted before persistence. NEVER log. */
    @Pattern(regexp = "^[0-9]{9}$", message = "Routing number must be exactly 9 digits")
    private String routingNumber;

    @Size(max = 1000)
    private String notes;

    @DecimalMin(value = "0.01")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal amount;

    @Size(min = 3, max = 3)
    private String currency;

    private PaymentFrequency frequency;

    @FutureOrPresent
    private LocalDate nextDueDate;

    @Min(1)
    @Max(31)
    private Integer dueDayOfMonth;

    private Boolean autoExecute;

    @Min(0)
    @Max(30)
    private Integer reminderDaysBefore;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getPaymentName() {
        return paymentName;
    }

    public void setPaymentName(String paymentName) {
        this.paymentName = paymentName;
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

    public Boolean getAutoExecute() {
        return autoExecute;
    }

    public void setAutoExecute(Boolean autoExecute) {
        this.autoExecute = autoExecute;
    }

    public Integer getReminderDaysBefore() {
        return reminderDaysBefore;
    }

    public void setReminderDaysBefore(Integer reminderDaysBefore) {
        this.reminderDaysBefore = reminderDaysBefore;
    }
}
