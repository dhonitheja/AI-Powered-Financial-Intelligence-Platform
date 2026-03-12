package com.wealthix.plaid.controller;

import com.example.financial.dto.ApiResponse;
import com.wealthix.audit.model.AuditAction;
import com.wealthix.audit.service.AuditLogService;
import com.wealthix.plaid.model.PlaidExchangeRequest;
import com.wealthix.plaid.model.UserBankConnectionResponse;
import com.wealthix.plaid.service.PlaidService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/plaid")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PlaidController {

  private final PlaidService plaidService;
  private final com.example.financial.service.BankSyncService bankSyncService;
  private final AuditLogService auditLogService;

  /**
   * Step 1: Frontend calls this to get link_token
   */
  @PostMapping("/link-token")
  @RateLimiter(name = "plaid-sync", fallbackMethod = "rateLimitFallback")
  public ResponseEntity<ApiResponse<Map<String, String>>> createLinkToken(
      @AuthenticationPrincipal com.example.financial.security.UserDetailsImpl user,
      HttpServletRequest request) {

    String linkToken = plaidService.createLinkToken(user.getId());

    auditLogService.log(
        UUID.fromString(user.getId()),
        AuditAction.PLAID_LINK_TOKEN_CREATED,
        "PLAID", null, request);

    // Matching the frontend key name (link_token or linkToken)
    return ResponseEntity.ok(ApiResponse.success(Map.of("link_token", linkToken)));
  }

  /**
   * Step 2: Frontend sends public_token
   */
  @PostMapping("/exchange")
  @RateLimiter(name = "plaid-sync", fallbackMethod = "rateLimitFallback")
  public ResponseEntity<ApiResponse<?>> exchangeToken(
      @Valid @RequestBody PlaidExchangeRequest req,
      @AuthenticationPrincipal com.example.financial.security.UserDetailsImpl user,
      HttpServletRequest request) {

    List<UserBankConnectionResponse> connections =
        plaidService.exchangePublicToken(req.getPublicToken(), user.getId())
        .stream()
        .map(UserBankConnectionResponse::fromEntity)
        .collect(Collectors.toList());

    auditLogService.log(
        UUID.fromString(user.getId()),
        AuditAction.PLAID_BANK_CONNECTED,
        "BANK_CONNECTION", null, request);

    return ResponseEntity.ok(ApiResponse.success(connections));
  }

  /**
   * Step 3: Manually trigger transaction sync
   */
  @PostMapping("/sync")
  @RateLimiter(name = "plaid-sync", fallbackMethod = "rateLimitFallback")
  public ResponseEntity<ApiResponse<String>> syncTransactions(
      @AuthenticationPrincipal com.example.financial.security.UserDetailsImpl user,
      HttpServletRequest request) {

    plaidService.syncAllConnections(user.getId());

    auditLogService.log(
        UUID.fromString(user.getId()),
        AuditAction.PLAID_SYNC_TRIGGERED,
        "PLAID", null, request);

    return ResponseEntity.ok(ApiResponse.success("Sync triggered successfully"));
  }

  /**
   * Get all linked bank accounts (masked)
   */
  @GetMapping("/connections")
  public ResponseEntity<ApiResponse<?>> getConnections(
      @AuthenticationPrincipal com.example.financial.security.UserDetailsImpl user) {
    return ResponseEntity.ok(
        ApiResponse.success(plaidService.getConnections(user.getId())));
  }

  /**
   * Returns financial position summary
   */
  @GetMapping("/accounts")
  public ResponseEntity<ApiResponse<com.example.financial.dto.FinancialSummaryDTO>> getAccountSummary(
      @AuthenticationPrincipal com.example.financial.security.UserDetailsImpl user) {
    return ResponseEntity.ok(
        ApiResponse.success(plaidService.getFinancialSummary(user.getId())));
  }

  /**
   * Lightweight polling endpoint for sync status
   */
  @GetMapping("/sync-status")
  public ResponseEntity<ApiResponse<com.example.financial.service.BankSyncService.SyncStatusDTO>> getSyncStatus(
      @AuthenticationPrincipal com.example.financial.security.UserDetailsImpl user) {
    return ResponseEntity.ok(
        ApiResponse.success(bankSyncService.getSyncStatus(user.getId())));
  }

  /**
   * Disconnect a bank account
   */
  @DeleteMapping("/connections/{connectionId}")
  public ResponseEntity<ApiResponse<String>> disconnectBank(
      @PathVariable UUID connectionId,
      @AuthenticationPrincipal com.example.financial.security.UserDetailsImpl user,
      HttpServletRequest request) {

    plaidService.disconnectBank(connectionId, user.getId());

    auditLogService.log(
        UUID.fromString(user.getId()),
        AuditAction.PLAID_BANK_DISCONNECTED,
        "BANK_CONNECTION", connectionId, request);

    return ResponseEntity.ok(ApiResponse.success("Bank account disconnected"));
  }

  public ResponseEntity<ApiResponse<?>> rateLimitFallback(
      com.example.financial.security.UserDetailsImpl user, HttpServletRequest request, Throwable t) {
      return ResponseEntity.status(429).body(
          ApiResponse.error("Slow down! Please wait a minute before trying again."));
  }
}
