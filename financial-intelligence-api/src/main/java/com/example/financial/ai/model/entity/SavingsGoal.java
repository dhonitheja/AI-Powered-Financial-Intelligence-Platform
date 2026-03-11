package com.example.financial.ai.model.entity;

import com.example.financial.entity.AppUser;
import com.example.financial.entity.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "savings_goals")
public class SavingsGoal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "goal_name", nullable = false)
    private String goalName;

    @Column(name = "target_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "current_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Column(length = 3)
    private String currency = "USD";

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "is_active")
    private Boolean isActive = true;

    public SavingsGoal() {}

    public SavingsGoal(AppUser user, String goalName, BigDecimal targetAmount, String currency, LocalDate targetDate) {
        this.user = user;
        this.goalName = goalName;
        this.targetAmount = targetAmount;
        if (currency != null) this.currency = currency;
        this.targetDate = targetDate;
    }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public String getGoalName() { return goalName; }
    public void setGoalName(String goalName) { this.goalName = goalName; }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public BigDecimal getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
}
