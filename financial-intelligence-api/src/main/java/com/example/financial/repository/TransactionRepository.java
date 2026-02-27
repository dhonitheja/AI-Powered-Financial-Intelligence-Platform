package com.example.financial.repository;

import com.example.financial.dto.CategorySpendingDTO;
import com.example.financial.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

        // ─── Duplicate-check ──────────────────────────────────────────────────────

        boolean existsByPlaidTransactionId(String plaidTransactionId);

        // ─── All-time (no filter) ─────────────────────────────────────────────────

        @Query("SELECT new com.example.financial.dto.CategorySpendingDTO(t.category, SUM(t.amount)) " +
                        "FROM Transaction t GROUP BY t.category ORDER BY SUM(t.amount) ASC")
        List<CategorySpendingDTO> calculateTotalSpendingByCategory();

        // ─── Date range only ──────────────────────────────────────────────────────

        @Query("SELECT t FROM Transaction t " +
                        "WHERE t.transactionDate >= :start AND t.transactionDate <= :end " +
                        "ORDER BY t.transactionDate DESC")
        List<Transaction> findByDateBetween(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT new com.example.financial.dto.CategorySpendingDTO(t.category, SUM(t.amount)) " +
                        "FROM Transaction t " +
                        "WHERE t.transactionDate >= :start AND t.transactionDate <= :end " +
                        "GROUP BY t.category ORDER BY SUM(t.amount) ASC")
        List<CategorySpendingDTO> calculateSpendingByCategoryBetween(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        // ─── Account type only ─────────────────────────────────────────────────────

        /**
         * Filters by a list of accountType values.
         * credit → ['CREDIT']
         * debit → ['CHECKING', 'SAVINGS']
         */
        @Query("SELECT t FROM Transaction t " +
                        "WHERE t.accountType IN :types " +
                        "ORDER BY t.transactionDate DESC")
        List<Transaction> findByAccountTypeIn(@Param("types") List<String> types);

        @Query("SELECT new com.example.financial.dto.CategorySpendingDTO(t.category, SUM(t.amount)) " +
                        "FROM Transaction t " +
                        "WHERE t.accountType IN :types " +
                        "GROUP BY t.category ORDER BY SUM(t.amount) ASC")
        List<CategorySpendingDTO> calculateSpendingByCategoryForTypes(@Param("types") List<String> types);

        // ─── Date range AND account type ──────────────────────────────────────────

        @Query("SELECT t FROM Transaction t " +
                        "WHERE t.transactionDate >= :start AND t.transactionDate <= :end " +
                        "AND t.accountType IN :types " +
                        "ORDER BY t.transactionDate DESC")
        List<Transaction> findByDateBetweenAndAccountTypeIn(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end,
                        @Param("types") List<String> types);

        @Query("SELECT new com.example.financial.dto.CategorySpendingDTO(t.category, SUM(t.amount)) " +
                        "FROM Transaction t " +
                        "WHERE t.transactionDate >= :start AND t.transactionDate <= :end " +
                        "AND t.accountType IN :types " +
                        "GROUP BY t.category ORDER BY SUM(t.amount) ASC")
        List<CategorySpendingDTO> calculateSpendingByCategoryBetweenForTypes(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end,
                        @Param("types") List<String> types);
}
