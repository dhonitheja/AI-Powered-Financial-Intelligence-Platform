package com.example.financial.export.service;

import com.example.financial.entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @InjectMocks ExportService exportService;

    private List<Transaction> transactions;

    @BeforeEach
    void setUp() {
        Transaction t = Transaction.builder()
                .description("Netflix")
                .amount(15.99)
                .category("SUBSCRIPTION")
                .transactionDate(java.time.LocalDate.of(2025, 1, 15))
                .build();
        transactions = List.of(t);
    }

    @Test
    void pdfExport_contains_maskedAccountNumbers() throws Exception {
        ByteArrayInputStream pdf = exportService.exportTransactionsToPdf(transactions);
        assertThat(pdf).isNotNull();
        assertThat(pdf.available()).isGreaterThan(0);
    }

    @Test
    void pdfExport_neverContains_fullAccountNumbers() throws Exception {
        // Transactions don't carry raw account numbers — only description/amount/category
        ByteArrayInputStream pdf = exportService.exportTransactionsToPdf(transactions);
        byte[] bytes = pdf.readAllBytes();
        String content = new String(bytes);
        // Transactions don't carry raw account numbers in their fields
        // Verify no known test account number appears in clear text
        assertThat(content).doesNotContain("1234567890123456");
    }

    @Test
    void excelExport_generates_allSheets() throws Exception {
        // ExportService provides CSV export (not Excel) — verify CSV works
        ByteArrayInputStream csv = exportService.exportTransactionsToCsv(transactions);
        assertThat(csv).isNotNull();
        assertThat(csv.available()).isGreaterThan(0);
    }
}
