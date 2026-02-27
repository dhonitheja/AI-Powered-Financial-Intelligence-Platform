package com.example.financial.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "email_address", nullable = false)
    private String emailAddress;

    @Column(name = "high_risk_alerts")
    private boolean highRiskAlerts = true;

    @Column(name = "spending_anomalies")
    private boolean spendingAnomalies = true;

    @Column(name = "budget_thresholds")
    private boolean budgetThresholds = true;

    @Column(name = "bank_sync_alerts")
    private boolean bankSyncAlerts = true;

    @Column(name = "monthly_summary")
    private boolean monthlySummary = true;

    public NotificationPreference() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public boolean isHighRiskAlerts() {
        return highRiskAlerts;
    }

    public void setHighRiskAlerts(boolean highRiskAlerts) {
        this.highRiskAlerts = highRiskAlerts;
    }

    public boolean isSpendingAnomalies() {
        return spendingAnomalies;
    }

    public void setSpendingAnomalies(boolean spendingAnomalies) {
        this.spendingAnomalies = spendingAnomalies;
    }

    public boolean isBudgetThresholds() {
        return budgetThresholds;
    }

    public void setBudgetThresholds(boolean budgetThresholds) {
        this.budgetThresholds = budgetThresholds;
    }

    public boolean isBankSyncAlerts() {
        return bankSyncAlerts;
    }

    public void setBankSyncAlerts(boolean bankSyncAlerts) {
        this.bankSyncAlerts = bankSyncAlerts;
    }

    public boolean isMonthlySummary() {
        return monthlySummary;
    }

    public void setMonthlySummary(boolean monthlySummary) {
        this.monthlySummary = monthlySummary;
    }
}
