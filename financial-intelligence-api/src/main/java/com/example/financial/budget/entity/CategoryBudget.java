package com.example.financial.budget.entity;

import com.example.financial.entity.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "category_budgets",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_budget_user_category_period",
                columnNames = {"user_id", "category", "period"}))
public class CategoryBudget extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "limit_amount", nullable = false)
    private Double limitAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BudgetPeriod period;

    @Column(nullable = false)
    private boolean active = true;

    // ── Constructors ──────────────────────────────────────────────────────────

    public CategoryBudget() {}

    public CategoryBudget(UUID userId, String category, Double limitAmount, BudgetPeriod period) {
        this.userId = userId;
        this.category = category;
        this.limitAmount = limitAmount;
        this.period = period;
        this.active = true;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getLimitAmount() { return limitAmount; }
    public void setLimitAmount(Double limitAmount) { this.limitAmount = limitAmount; }

    public BudgetPeriod getPeriod() { return period; }
    public void setPeriod(BudgetPeriod period) { this.period = period; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
