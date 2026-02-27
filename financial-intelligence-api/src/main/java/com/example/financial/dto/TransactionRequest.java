package com.example.financial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public class TransactionRequest {

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotNull(message = "Transaction date is required")
    private LocalDateTime transactionDate;

    @NotBlank(message = "Category is required")
    private String category;

    public TransactionRequest() {
    }

    public TransactionRequest(String description, Double amount, LocalDateTime transactionDate, String category) {
        this.description = description;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.category = category;
    }

    public static class TransactionRequestBuilder {
        private String description;
        private Double amount;
        private LocalDateTime transactionDate;
        private String category;

        public TransactionRequestBuilder description(String description) {
            this.description = description;
            return this;
        }

        public TransactionRequestBuilder amount(Double amount) {
            this.amount = amount;
            return this;
        }

        public TransactionRequestBuilder transactionDate(LocalDateTime transactionDate) {
            this.transactionDate = transactionDate;
            return this;
        }

        public TransactionRequestBuilder category(String category) {
            this.category = category;
            return this;
        }

        public TransactionRequest build() {
            return new TransactionRequest(description, amount, transactionDate, category);
        }
    }

    public static TransactionRequestBuilder builder() {
        return new TransactionRequestBuilder();
    }

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
}
