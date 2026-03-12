package com.example.financial.budget.repository;

import com.example.financial.budget.entity.BudgetPeriod;
import com.example.financial.budget.entity.CategoryBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryBudgetRepository extends JpaRepository<CategoryBudget, UUID> {

    List<CategoryBudget> findByUserIdAndActiveTrue(UUID userId);

    Optional<CategoryBudget> findByIdAndUserId(UUID id, UUID userId);

    Optional<CategoryBudget> findByUserIdAndCategoryIgnoreCaseAndPeriod(
            UUID userId, String category, BudgetPeriod period);
}
