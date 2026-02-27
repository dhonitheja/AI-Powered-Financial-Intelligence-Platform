package com.example.financial.dto;

/**
 * Represents a single linked financial account (bank or credit card).
 */
public class AccountSummaryDTO {

    private String accountId;
    private String accountName;
    private String institutionName;
    private String accountType; // CHECKING, SAVINGS, CREDIT
    private Double currentBalance;
    private Double creditLimit;
    private Double utilizationPercent; // null for non-credit accounts

    public AccountSummaryDTO() {
    }

    public AccountSummaryDTO(String accountId, String accountName, String institutionName,
            String accountType, Double currentBalance,
            Double creditLimit, Double utilizationPercent) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.institutionName = institutionName;
        this.accountType = accountType;
        this.currentBalance = currentBalance;
        this.creditLimit = creditLimit;
        this.utilizationPercent = utilizationPercent;
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

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
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

    public Double getUtilizationPercent() {
        return utilizationPercent;
    }

    public void setUtilizationPercent(Double utilizationPercent) {
        this.utilizationPercent = utilizationPercent;
    }
}
