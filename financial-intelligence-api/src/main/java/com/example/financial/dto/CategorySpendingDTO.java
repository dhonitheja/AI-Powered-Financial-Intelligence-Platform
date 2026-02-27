package com.example.financial.dto;

public class CategorySpendingDTO {
    private String category;
    private Double totalSpending;

    public CategorySpendingDTO() {
    }

    public CategorySpendingDTO(String category, Double totalSpending) {
        this.category = category;
        this.totalSpending = totalSpending;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getTotalSpending() {
        return totalSpending;
    }

    public void setTotalSpending(Double totalSpending) {
        this.totalSpending = totalSpending;
    }
}
