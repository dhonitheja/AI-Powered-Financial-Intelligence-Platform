package com.example.financial.budget.controller;

import com.example.financial.budget.dto.CategoryBudgetRequest;
import com.example.financial.budget.dto.CategoryBudgetResponse;
import com.example.financial.budget.service.CategoryBudgetService;
import com.example.financial.security.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets")
public class CategoryBudgetController {

    private final CategoryBudgetService budgetService;

    public CategoryBudgetController(CategoryBudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryBudgetResponse>> getBudgets(
            @AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(
                budgetService.getBudgetsForUser(UUID.fromString(user.getId())));
    }

    @PostMapping
    public ResponseEntity<CategoryBudgetResponse> createBudget(
            @Valid @RequestBody CategoryBudgetRequest request,
            @AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                budgetService.createBudget(UUID.fromString(user.getId()), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryBudgetResponse> updateBudget(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryBudgetRequest request,
            @AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(
                budgetService.updateBudget(id, UUID.fromString(user.getId()), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl user) {
        budgetService.deleteBudget(id, UUID.fromString(user.getId()));
        return ResponseEntity.noContent().build();
    }
}
