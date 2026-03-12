package com.example.financial.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String description;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false)
    private String category;

    @Column(name = "fraud_risk_score")
    private Double fraudRiskScore;

    @Column(name = "ai_explanation", length = 1000)
    private String aiExplanation;

    @Column(name = "plaid_transaction_id", unique = true)
    private String plaidTransactionId;

    @Column(name = "plaid_account_id")
    private String plaidAccountId;

    @Column(name = "account_type", length = 20)
    private String accountType = "CHECKING";

    @Column(nullable = false)
    private boolean pending = false;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
    
    // Compatibility for legacy code
    public void setAccountId(String accountId) { this.plaidAccountId = accountId; }
    public String getAccountId() { return plaidAccountId; }
}
