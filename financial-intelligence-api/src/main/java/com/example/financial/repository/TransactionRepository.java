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

        @Query("""
                        SELECT t FROM Transaction t
                        WHERE t.accountId IN :accountIds
                          AND t.deleted = false
                        ORDER BY t.transactionDate DESC
                        """)
        List<Transaction> findByAccountIds(@Param("accountIds") List<String> accountIds);

        /**
         * Finds transactions for a set of account IDs within a date range.
         * Used for recurring-payment detection (need 90-day window).
         */
        @Query("""
                        SELECT t FROM Transaction t
                        WHERE t.accountId IN :accountIds
                          AND t.transactionDate >= :start
                          AND t.transactionDate <= :end
                          AND t.amount < 0
                          AND t.deleted = false
                        ORDER BY t.description, t.transactionDate
                        """)
        List<Transaction> findNegativeByAccountIdsBetween(
                        @Param("accountIds") List<String> accountIds,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        /**
         * Finds transactions to a specific merchant (description match) in a
         * date range — used for duplicate-payment detection.
         */
        @Query("""
                        SELECT t FROM Transaction t
                        WHERE t.accountId IN :accountIds
                          AND LOWER(t.description) LIKE LOWER(CONCAT('%', :merchant, '%'))
                          AND t.transactionDate >= :cycleStart
                          AND t.transactionDate <= :cycleEnd
                          AND t.amount < 0
                          AND t.deleted = false
                        ORDER BY t.transactionDate DESC
                        """)
        List<Transaction> findByMerchantInCycle(
                        @Param("accountIds") List<String> accountIds,
                        @Param("merchant") String merchant,
                        @Param("cycleStart") LocalDateTime cycleStart,
                        @Param("cycleEnd") LocalDateTime cycleEnd);

        /**
         * Finds transactions matching a Plaid transaction ID after execution —
         * used for Plaid-verification of Stripe payments.
         */
        @Query("""
                        SELECT t FROM Transaction t
                        WHERE t.accountId IN :accountIds
                          AND ABS(t.amount) BETWEEN :minAmount AND :maxAmount
                          AND t.transactionDate >= :after
                          AND t.deleted = false
                        ORDER BY t.transactionDate ASC
                        """)
        List<Transaction> findByAmountRangeAfter(
                        @Param("accountIds") List<String> accountIds,
                        @Param("minAmount") double minAmount,
                        @Param("maxAmount") double maxAmount,
                        @Param("after") LocalDateTime after);
}
