package com.wealthix.service;

import com.wealthix.entity.Transaction;
import com.wealthix.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BulkImportIntegrationTest {

    @Autowired
    private BulkImportService bulkImportService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void importTransactions_persistsToH2() {
        String csvContent = "description,amount,date,category\n" +
                            "Integrated Test,123.45,2025-01-01,INTEGRATION";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));
        UUID userId = UUID.randomUUID();

        List<Transaction> imported = bulkImportService.importTransactions(file, userId);
        assertThat(imported).hasSize(1);
        
        List<Transaction> dbRecords = transactionRepository.findAll();
        assertThat(dbRecords).anyMatch(t -> t.getDescription().equals("Integrated Test") && t.getUserId().equals(userId));
    }
}
