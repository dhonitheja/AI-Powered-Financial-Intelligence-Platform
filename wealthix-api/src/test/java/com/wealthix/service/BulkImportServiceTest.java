package com.wealthix.service;

import com.wealthix.entity.Transaction;
import com.wealthix.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkImportServiceTest {

    @Mock TransactionRepository transactionRepository;
    @InjectMocks BulkImportService bulkImportService;

    @Test
    void importTransactions_success() {
        String csvContent = "description,amount,date,category\n" +
                            "Netflix,15.99,2025-01-01,SUBSCRIPTION\n" +
                            "Rent,1200.00,2025-01-02,LIVING";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));
        UUID userId = UUID.randomUUID();

        when(transactionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Transaction> imported = bulkImportService.importTransactions(file, userId);

        assertThat(imported).hasSize(2);
        assertThat(imported.get(0).getDescription()).isEqualTo("Netflix");
        assertThat(imported.get(0).getUserId()).isEqualTo(userId);
    }

    @Test
    void importTransactions_sanitizesDescription() {
        String csvContent = "description,amount,date\n" +
                            "<script>alert(1)</script>Safe,10.00,2025-01-01";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));
        
        when(transactionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Transaction> imported = bulkImportService.importTransactions(file, UUID.randomUUID());

        assertThat(imported.get(0).getDescription()).isEqualTo("alert(1)Safe");
    }

    @Test
    void importTransactions_rejectsLargeFile() {
        StringBuilder sb = new StringBuilder("description,amount,date\n");
        for (int i = 0; i < 1001; i++) {
            sb.append("Item,10.00,2025-01-01\n");
        }
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", sb.toString().getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> bulkImportService.importTransactions(file, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Maximum of 1000 records");
    }
}
