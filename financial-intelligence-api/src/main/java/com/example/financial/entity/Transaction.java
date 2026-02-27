package com.example.financial.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction extends BaseEntity {

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(nullable = false)
    private String category;

    @Column(name = "fraud_risk_score")
    private Double fraudRiskScore;

    @Column(name = "ai_explanation", length = 1000)
    private String aiExplanation;

    @Column(name = "plaid_transaction_id", unique = true)
    private String plaidTransactionId;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    /**
     * Plaid account ID this transaction originated from.
     * Null for manually-entered transactions.
     */
    @Column(name = "account_id")
    private String accountId;

    /**
     * Account type: CHECKING, SAVINGS, CREDIT.
     * Defaults to CHECKING for manually-entered transactions.
     * Used for credit/debit filtering at the database level.
     */
    @Column(name = "account_type", length = 20)
    private String accountType = "CHECKING";

    public Transaction() {
    }

    public Transaction(String description, Double amount, LocalDateTime transactionDate, String category) {
        this.description = description;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.category = category;
    }

    // ── Builder ────────────────────────────────────────────────────────────────

    public static class TransactionBuilder {
        private String description;
        private Double amount;
        private LocalDateTime transactionDate;
        private String category;
        private Double fraudRiskScore;
        private String aiExplanation;
        private String plaidTransactionId;
        private String accountId;
        private String accountType;

        public TransactionBuilder description(String description) {
            this.description = description;
            return this;
        }

        public TransactionBuilder amount(Double amount) {
            this.amount = amount;
            return this;
        }

        public TransactionBuilder transactionDate(LocalDateTime transactionDate) {
            this.transactionDate = transactionDate;
            return this;
        }

        public TransactionBuilder category(String category) {
            this.category = category;
            return this;
        }

        public TransactionBuilder fraudRiskScore(Double fraudRiskScore) {
            this.fraudRiskScore = fraudRiskScore;
            return this;
        }

        public TransactionBuilder aiExplanation(String aiExplanation) {
            this.aiExplanation = aiExplanation;
            return this;
        }

        public TransactionBuilder plaidTransactionId(String plaidTransactionId) {
            this.plaidTransactionId = plaidTransactionId;
            return this;
        }

        public TransactionBuilder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public TransactionBuilder accountType(String accountType) {
            this.accountType = accountType;
            return this;
        }

        public Transaction build() {
            Transaction t = new Transaction();
            t.setDescription(description);
            t.setAmount(amount);
            t.setTransactionDate(transactionDate);
            t.setCategory(category);
            t.setFraudRiskScore(fraudRiskScore);
            t.setAiExplanation(aiExplanation);
            t.setPlaidTransactionId(plaidTransactionId);
            t.setAccountId(accountId);
            t.setAccountType(accountType != null ? accountType : "CHECKING");
            return t;
        }
    }

    public static TransactionBuilder builder() {
        return new TransactionBuilder();
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getFraudRiskScore() {
        return fraudRiskScore;
    }

    public void setFraudRiskScore(Double fraudRiskScore) {
        this.fraudRiskScore = fraudRiskScore;
    }

    public String getAiExplanation() {
        return aiExplanation;
    }

    public void setAiExplanation(String aiExplanation) {
        this.aiExplanation = aiExplanation;
    }

    public String getPlaidTransactionId() {
        return plaidTransactionId;
    }

    public void setPlaidTransactionId(String plaidTransactionId) {
        this.plaidTransactionId = plaidTransactionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
}
