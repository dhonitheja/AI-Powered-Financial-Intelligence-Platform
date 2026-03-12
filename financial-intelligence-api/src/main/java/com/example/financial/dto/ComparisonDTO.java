package com.example.financial.dto;

import java.util.List;

public class ComparisonDTO {

    private String period;
    private String currentPeriodLabel;
    private String previousPeriodLabel;
    private double currentTotal;
    private double previousTotal;
    private List<CategoryComparison> categories;

    public record CategoryComparison(
            String category,
            double currentPeriodSpend,
            double previousPeriodSpend,
            double changePercent
    ) {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public String getCurrentPeriodLabel() { return currentPeriodLabel; }
    public void setCurrentPeriodLabel(String currentPeriodLabel) { this.currentPeriodLabel = currentPeriodLabel; }

    public String getPreviousPeriodLabel() { return previousPeriodLabel; }
    public void setPreviousPeriodLabel(String previousPeriodLabel) { this.previousPeriodLabel = previousPeriodLabel; }

    public double getCurrentTotal() { return currentTotal; }
    public void setCurrentTotal(double currentTotal) { this.currentTotal = currentTotal; }

    public double getPreviousTotal() { return previousTotal; }
    public void setPreviousTotal(double previousTotal) { this.previousTotal = previousTotal; }

    public List<CategoryComparison> getCategories() { return categories; }
    public void setCategories(List<CategoryComparison> categories) { this.categories = categories; }
}
