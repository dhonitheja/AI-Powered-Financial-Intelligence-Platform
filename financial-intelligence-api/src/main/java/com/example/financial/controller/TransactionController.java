package com.example.financial.controller;

import com.example.financial.dto.CategorySpendingDTO;
import com.example.financial.dto.TransactionRequest;
import com.example.financial.entity.Transaction;
import com.example.financial.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Transactions", description = "Transaction management and analysis endpoints")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transactions")
    @Operation(summary = "Create a new transaction")
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody TransactionRequest request) {
        // userId resolved from SecurityContext — never from request body
        String userId = resolveUserId();
        Transaction transaction = Transaction.builder()
                .description(request.getDescription())
                .amount(request.getAmount())
                .transactionDate(request.getTransactionDate())
                .category(request.getCategory())
                .accountType("CHECKING") // manual transactions default to CHECKING
                .build();
        return new ResponseEntity<>(transactionService.createTransaction(transaction), HttpStatus.CREATED);
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get transactions, optionally filtered by time period and/or account type")
    public ResponseEntity<List<Transaction>> getAllTransactions(
            @Parameter(description = "Time period: weekly | monthly | 6months | yearly | omit for all time") @RequestParam(name = "period", required = false) String period,
            @Parameter(description = "Account type filter: credit | debit | omit for all types") @RequestParam(name = "type", required = false) String type) {
        return ResponseEntity.ok(transactionService.getAllTransactions(period, type));
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
    public ResponseEntity<List<CategorySpendingDTO>> getFinancialSummary(
            @Parameter(description = "Time period: weekly | monthly | 6months | yearly | omit for all time") @RequestParam(name = "period", required = false) String period,
            @Parameter(description = "Account type filter: credit | debit | omit for all types") @RequestParam(name = "type", required = false) String type) {
        return ResponseEntity.ok(transactionService.getFinancialSummary(period, type));
    }

    @GetMapping("/summary/range")
    @Operation(summary = "Get category spending for an explicit date range")
    public ResponseEntity<List<CategorySpendingDTO>> getSummaryForRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(
                transactionService.getFinancialSummaryForRange(startDate, endDate));
    }

    // ── Resolve authenticated user from SecurityContext (never from request) ───
    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";
    }
}
