package com.example.financial.budget.service;

import com.example.financial.budget.dto.CategoryBudgetRequest;
import com.example.financial.budget.dto.CategoryBudgetResponse;
import com.example.financial.budget.entity.CategoryBudget;
import com.example.financial.budget.repository.CategoryBudgetRepository;
import com.example.financial.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CategoryBudgetService {

    private final CategoryBudgetRepository budgetRepository;

    public CategoryBudgetService(CategoryBudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryBudgetResponse> getBudgetsForUser(UUID userId) {
        return budgetRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(CategoryBudgetResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryBudgetResponse createBudget(UUID userId, CategoryBudgetRequest request) {
        // Upsert: reuse existing entry for same (user, category, period) if one exists
        CategoryBudget budget = budgetRepository
                .findByUserIdAndCategoryIgnoreCaseAndPeriod(userId, request.getCategory(), request.getPeriod())
                .orElseGet(() -> new CategoryBudget(userId, request.getCategory(), request.getLimitAmount(), request.getPeriod()));

        budget.setLimitAmount(request.getLimitAmount());
        budget.setCategory(request.getCategory().toUpperCase());
        budget.setActive(true);
        return CategoryBudgetResponse.fromEntity(budgetRepository.save(budget));
    }

    @Transactional
    public CategoryBudgetResponse updateBudget(UUID budgetId, UUID userId, CategoryBudgetRequest request) {
        CategoryBudget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + budgetId));

        budget.setCategory(request.getCategory().toUpperCase());
        budget.setLimitAmount(request.getLimitAmount());
        budget.setPeriod(request.getPeriod());
        return CategoryBudgetResponse.fromEntity(budgetRepository.save(budget));
    }

    @Transactional
    public void deleteBudget(UUID budgetId, UUID userId) {
        CategoryBudget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + budgetId));
        budget.setActive(false);
        budgetRepository.save(budget);
    }
}
