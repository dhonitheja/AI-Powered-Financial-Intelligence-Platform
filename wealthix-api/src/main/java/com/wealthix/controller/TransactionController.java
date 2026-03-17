package com.wealthix.controller;

import com.wealthix.dto.CategorySpendingDTO;
import com.wealthix.dto.TransactionRequest;
import com.wealthix.entity.Transaction;
import com.wealthix.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.wealthix.security.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Transactions", description = "Transaction management and analysis endpoints")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transactions")
    @Operation(summary = "Create a new transaction")
    public ResponseEntity<?> createTransaction(@Valid @RequestBody TransactionRequest request) {
        try {
            UUID userId = resolveUserUuid();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not logged in"));
            }

            Transaction transaction = Transaction.builder()
                    .userId(userId)
                    .description(request.getDescription())
                    .amount(request.getAmount())
                    .transactionDate(request.getTransactionDate().toLocalDate())
                    .category(request.getCategory())
                    .accountType("CHECKING") // manual transactions default to CHECKING
                    .build();
            return new ResponseEntity<>(transactionService.createTransaction(transaction), HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("[TxController] Failed to create transaction: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create transaction", "message", e.getMessage()));
        }
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get transactions, optionally filtered by time period and/or account type")
    public ResponseEntity<?> getAllTransactions(
            @Parameter(description = "Time period: weekly | monthly | 6months | yearly | omit for all time") @RequestParam(name = "period", required = false) String period,
            @Parameter(description = "Account type filter: credit | debit | omit for all types") @RequestParam(name = "type", required = false) String type) {
        try {
            UUID userId = resolveUserUuid();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not logged in"));
            }
            log.info("[TxController] GET /transactions user={} period={} type={}", userId, period, type);
            List<Transaction> transactions = transactionService.getAllTransactions(userId, period, type);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("[TxController] Failed to fetch transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch transactions", "message", e.getMessage()));
        }
    }

    @GetMapping("/transactions/{id}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    @PostMapping("/transactions/{id}/analyze")
    @Operation(summary = "Analyze transaction for fraud risk (async AI analysis)")
    public ResponseEntity<Map<String, String>> analyzeTransaction(@PathVariable("id") UUID id) {
        transactionService.analyzeTransactionAsync(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Analysis started for transaction " + id);
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get financial summary by category, optionally filtered by time period and/or type")
    public ResponseEntity<?> getFinancialSummary(
            @Parameter(description = "Time period: weekly | monthly | 6months | yearly | omit for all time") @RequestParam(name = "period", required = false) String period,
            @Parameter(description = "Account type filter: credit | debit | omit for all types") @RequestParam(name = "type", required = false) String type) {
        try {
            UUID userId = resolveUserUuid();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not logged in"));
            }
            log.info("[TxController] GET /summary user={} period={} type={}", userId, period, type);
            List<CategorySpendingDTO> summary = transactionService.getFinancialSummary(userId, period, type);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("[TxController] Failed to fetch summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch summary", "message", e.getMessage()));
        }
    }

    @GetMapping("/summary/range")
    @Operation(summary = "Get category spending for an explicit date range")
    public ResponseEntity<?> getSummaryForRange(
            @RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UUID userId = resolveUserUuid();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not logged in"));
        }
        return ResponseEntity.ok(
                transactionService.getFinancialSummaryForRange(userId, startDate, endDate));
    }

    // ── Resolve authenticated user from SecurityContext ────────────────────────
    private UUID resolveUserUuid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl) {
            String idStr = ((UserDetailsImpl) auth.getPrincipal()).getId();
            return (idStr != null) ? UUID.fromString(idStr) : null;
        }
        return null;
    }
}
