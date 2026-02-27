package com.example.financial.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_bank_connections")
public class UserBankConnection extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "encrypted_access_token", nullable = false, length = 1000)
    private String encryptedAccessToken;

    @Column(name = "next_cursor", length = 1000)
    private String nextCursor;

    @Column(name = "item_id")
    private String itemId;

    @Column(name = "institution_name")
    private String institutionName;

    // ── New: account-level fields ──────────────────────────────────────────────

    /** Plaid account ID for this specific account sub-entry */
    @Column(name = "account_id")
    private String accountId;

    /**
     * Human-readable name from Plaid (e.g. "Plaid Savings", "Plaid Credit Card")
     */
    @Column(name = "account_name")
    private String accountName;

    /**
     * Account type derived from Plaid account type/subtype.
     * Values: CHECKING, SAVINGS, CREDIT
     */
    @Column(name = "account_type", length = 20)
    private String accountType = "CHECKING";

    /** Current balance in dollars. For credit: how much is owed (positive). */
    @Column(name = "current_balance")
    private Double currentBalance;

    /** Available credit limit — only set for CREDIT accounts, null otherwise. */
    @Column(name = "credit_limit")
    private Double creditLimit;

    public UserBankConnection() {
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEncryptedAccessToken() {
        return encryptedAccessToken;
    }

    public void setEncryptedAccessToken(String encryptedAccessToken) {
        this.encryptedAccessToken = encryptedAccessToken;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public Double getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(Double currentBalance) {
        this.currentBalance = currentBalance;
    }

    public Double getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(Double creditLimit) {
        this.creditLimit = creditLimit;
    }
}
