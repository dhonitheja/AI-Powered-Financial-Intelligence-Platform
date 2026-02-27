package com.example.financial.dto;

import java.util.List;

/**
 * Rolled-up financial position across all linked accounts.
 */
public class FinancialSummaryDTO {

    private double totalAssets; // Sum of all CHECKING + SAVINGS balances
    private double totalLiabilities; // Sum of all CREDIT balances owed
    private double netWorth; // totalAssets - totalLiabilities
    private double totalCreditLimit; // Aggregate credit limit across cards
    private double creditUtilization; // totalLiabilities / totalCreditLimit * 100 (%)
    private int accountCount;
    private List<AccountSummaryDTO> accounts;

    public FinancialSummaryDTO() {
    }

    public FinancialSummaryDTO(double totalAssets, double totalLiabilities, double totalCreditLimit,
            List<AccountSummaryDTO> accounts) {
        this.totalAssets = totalAssets;
        this.totalLiabilities = totalLiabilities;
        this.netWorth = totalAssets - totalLiabilities;
        this.totalCreditLimit = totalCreditLimit;
        this.creditUtilization = totalCreditLimit > 0
                ? Math.round((totalLiabilities / totalCreditLimit) * 1000.0) / 10.0 // one decimal
                : 0.0;
        this.accountCount = accounts != null ? accounts.size() : 0;
        this.accounts = accounts;
    }

    public double getTotalAssets() {
        return totalAssets;
    }

    public void setTotalAssets(double totalAssets) {
        this.totalAssets = totalAssets;
    }

    public double getTotalLiabilities() {
        return totalLiabilities;
    }

    public void setTotalLiabilities(double totalLiabilities) {
        this.totalLiabilities = totalLiabilities;
    }

    public double getNetWorth() {
        return netWorth;
    }

    public void setNetWorth(double netWorth) {
        this.netWorth = netWorth;
    }

    public double getTotalCreditLimit() {
        return totalCreditLimit;
    }

    public void setTotalCreditLimit(double totalCreditLimit) {
        this.totalCreditLimit = totalCreditLimit;
    }

    public double getCreditUtilization() {
        return creditUtilization;
    }

    public void setCreditUtilization(double creditUtilization) {
        this.creditUtilization = creditUtilization;
    }

    public int getAccountCount() {
        return accountCount;
    }

    public void setAccountCount(int accountCount) {
        this.accountCount = accountCount;
    }

    public List<AccountSummaryDTO> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AccountSummaryDTO> accounts) {
        this.accounts = accounts;
    }
}
