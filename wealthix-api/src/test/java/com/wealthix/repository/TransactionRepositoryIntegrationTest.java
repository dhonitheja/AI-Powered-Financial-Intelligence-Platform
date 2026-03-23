package com.wealthix.repository;

import com.wealthix.entity.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class TransactionRepositoryIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void testSaveAndRetrieveTransaction() {
        UUID userId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .description("H2 Test Transaction")
                .amount(250.75)
                .transactionDate(LocalDate.now())
                .category("TESTING")
                .build();

        Transaction saved = transactionRepository.save(transaction);
        assertThat(saved.getId()).isNotNull();

        List<Transaction> transactions = transactionRepository.findByDateBetween(userId, 
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getDescription()).isEqualTo("H2 Test Transaction");
        assertThat(transactions.get(0).getAmount()).isEqualTo(250.75);
    }
}
