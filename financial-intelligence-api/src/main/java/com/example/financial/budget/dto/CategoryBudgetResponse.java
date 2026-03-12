package com.example.financial.budget.dto;

import com.example.financial.budget.entity.BudgetPeriod;
import com.example.financial.budget.entity.CategoryBudget;

import java.time.LocalDateTime;
import java.util.UUID;

public class CategoryBudgetResponse {

    private UUID id;
    private String category;
    private Double limitAmount;
    private BudgetPeriod period;
    private boolean active;
    private LocalDateTime createdAt;

    public static CategoryBudgetResponse fromEntity(CategoryBudget b) {
        CategoryBudgetResponse r = new CategoryBudgetResponse();
        r.id = b.getId();
        r.category = b.getCategory();
        r.limitAmount = b.getLimitAmount();
        r.period = b.getPeriod();
        r.active = b.isActive();
        r.createdAt = b.getCreatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public String getCategory() { return category; }
    public Double getLimitAmount() { return limitAmount; }
    public BudgetPeriod getPeriod() { return period; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
