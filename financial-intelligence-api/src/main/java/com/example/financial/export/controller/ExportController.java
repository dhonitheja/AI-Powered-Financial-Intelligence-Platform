package com.example.financial.export.controller;

import com.example.financial.entity.Transaction;
import com.example.financial.export.service.ExportService;
import com.example.financial.service.TransactionService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/export")
public class ExportController {

    private final ExportService exportService;
    private final TransactionService transactionService;

    public ExportController(ExportService exportService,
                            TransactionService transactionService) {
        this.exportService = exportService;
        this.transactionService = transactionService;
    }

    private List<Transaction> getTransactionsForUser(Authentication auth, String period) {
        return transactionService.getAllTransactions(period);
    }

    @GetMapping("/csv")
    public ResponseEntity<Resource> exportCsv(Authentication auth,
                                              @RequestParam(required = false) String period) {
        if (auth == null) return ResponseEntity.status(401).build();

        List<Transaction> transactions = getTransactionsForUser(auth, period);
        ByteArrayInputStream out = exportService.exportTransactionsToCsv(transactions);

        String filename = "transactions_export.csv";
        InputStreamResource file = new InputStreamResource(out);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(file);
    }

    @GetMapping("/pdf")
    public ResponseEntity<Resource> exportPdf(Authentication auth,
                                              @RequestParam(required = false) String period) {
        if (auth == null) return ResponseEntity.status(401).build();

        List<Transaction> transactions = getTransactionsForUser(auth, period);
        ByteArrayInputStream out = exportService.exportTransactionsToPdf(transactions);

        String filename = "transactions_report.pdf";
        InputStreamResource file = new InputStreamResource(out);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }
}
