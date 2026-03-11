package com.example.financial.ai.controller;

import com.example.financial.ai.model.dto.ChatMessageDto;
import com.example.financial.ai.model.dto.ChatResponseDto;
import com.example.financial.ai.model.dto.CreateSavingsGoalRequest;
import com.example.financial.ai.service.AiAssistantService;
import com.example.financial.dto.ApiResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@PreAuthorize("isAuthenticated()")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/chat")
    @RateLimiter(name = "ai-chat", fallbackMethod = "rateLimitFallbackChat")
    public ResponseEntity<ApiResponse<ChatResponseDto>> chat(
            @Valid @RequestBody ChatMessageDto request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        aiAssistantService.chat(request, user.getUsername())));
    }

    public ResponseEntity<ApiResponse<ChatResponseDto>> rateLimitFallbackChat(
            ChatMessageDto request, UserDetails user, Throwable t) {
        return ResponseEntity.status(429).body(
            ApiResponse.error("Too many messages. Please try again later."));
    }

    @GetMapping("/forecast")
    public ResponseEntity<ApiResponse<?>> getForecast(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        aiAssistantService.getSpendingForecast(user.getUsername())));
    }

    @GetMapping("/anomalies")
    public ResponseEntity<ApiResponse<?>> getAnomalies(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        aiAssistantService.detectAnomalies(user.getUsername())));
    }

    @PostMapping("/anomalies/{id}/acknowledge")
    public ResponseEntity<ApiResponse<?>> acknowledgeAnomaly(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        aiAssistantService.acknowledgeAnomaly(id, user.getUsername());
        return ResponseEntity.ok(
                ApiResponse.success("Anomaly acknowledged"));
    }

    @GetMapping("/budget-recommendations")
    public ResponseEntity<ApiResponse<?>> getBudgetRecs(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        aiAssistantService.getBudgetRecommendations(user.getUsername())));
    }

    @GetMapping("/savings-goals")
    public ResponseEntity<ApiResponse<?>> getSavingsGoals(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        aiAssistantService.getSavingsGoals(user.getUsername())));
    }

    @PostMapping("/savings-goals")
    @RateLimiter(name = "autopay-write", fallbackMethod = "rateLimitFallbackGoals")
    public ResponseEntity<ApiResponse<?>> createSavingsGoal(
            @Valid @RequestBody CreateSavingsGoalRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(201).body(
                ApiResponse.success(
                        aiAssistantService.createSavingsGoal(req, user.getUsername())));
    }

    public ResponseEntity<ApiResponse<?>> rateLimitFallbackGoals(
            CreateSavingsGoalRequest req, UserDetails user, Throwable t) {
        return ResponseEntity.status(429).body(
            ApiResponse.error("Too many requests. Please try again later."));
    }
}
