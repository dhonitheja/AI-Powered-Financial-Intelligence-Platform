package com.wealthix.repository;

import com.wealthix.dto.CategorySpendingDTO;
import com.wealthix.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

        // ─── Duplicate-check ──────────────────────────────────────────────────────

        boolean existsByPlaidTransactionId(String plaidTransactionId);

        // ─── All-time (no filter) ─────────────────────────────────────────────────

        @Query("SELECT new com.wealthix.dto.CategorySpendingDTO(t.category, SUM(t.amount)) " +
                        "FROM Transaction t WHERE t.userId = :userId GROUP BY t.category ORDER BY SUM(t.amount) ASC")
        List<CategorySpendingDTO> calculateTotalSpendingByCategory(@Param("userId") UUID userId);

        // ─── Date range only ──────────────────────────────────────────────────────

        @Query("SELECT t FROM Transaction t " +
                        "WHERE t.userId = :userId AND t.transactionDate >= :start AND t.transactionDate <= :end " +
                        "ORDER BY t.transactionDate DESC")
        List<Transaction> findByDateBetween(
                        @Param("userId") UUID userId,
                        @Param("start") LocalDate start,
                        @Param("end") LocalDate end);

        @Query("SELECT new com.wealthix.dto.CategorySpendingDTO(t.category, SUM(t.amount)) " +
                        "FROM Transaction t " +
                        "WHERE t.userId = :userId AND t.transactionDate >= :start AND t.transactionDate <= :end " +
                        "GROUP BY t.category ORDER BY SUM(t.amount) ASC")
        List<CategorySpendingDTO> calculateSpendingByCategoryBetween(
                        @Param("userId") UUID userId,
                        @Param("start") LocalDate start,
                        @Param("end") LocalDate end);

        // ─── Account type only ─────────────────────────────────────────────────────

        /**
         * Filters by a list of accountType values.
         * credit → ['CREDIT']
         * debit → ['CHECKING', 'SAVINGS']
         */
        @Query("SELECT t FROM Transaction t " +
                        "WHERE t.userId = :userId AND t.accountType IN :types " +
                        "ORDER BY t.transactionDate DESC")
        List<Transaction> findByAccountTypeIn(@Param("userId") UUID userId, @Param("types") List<String> types);

        @Query("SELECT new com.wealthix.dto.CategorySpendingDTO(t.category, SUM(t.amount)) " +
                        "FROM Transaction t " +
                        "WHERE t.userId = :userId AND t.accountType IN :types " +
                        "GROUP BY t.category ORDER BY SUM(t.amount) ASC")
        List<CategorySpendingDTO> calculateSpendingByCategoryForTypes(@Param("userId") UUID userId, @Param("types") List<String> types);

        // ─── Date range AND account type ──────────────────────────────────────────

        @Query("SELECT t FROM Transaction t " +
                        "WHERE t.userId = :userId AND t.transactionDate >= :start AND t.transactionDate <= :end " +
                        "AND t.accountType IN :types " +
                        "ORDER BY t.transactionDate DESC")
        List<Transaction> findByDateBetweenAndAccountTypeIn(
                        @Param("userId") UUID userId,
                        @Param("start") LocalDate start,
                        @Param("end") LocalDate end,
                        @Param("types") List<String> types);

        @Query("SELECT new com.wealthix.dto.CategorySpendingDTO(t.category, SUM(t.amount)) " +
                        "FROM Transaction t " +
                        "WHERE t.userId = :userId AND t.transactionDate >= :start AND t.transactionDate <= :end " +
                        "AND t.accountType IN :types " +
                        "GROUP BY t.category ORDER BY SUM(t.amount) ASC")
        List<CategorySpendingDTO> calculateSpendingByCategoryBetweenForTypes(
                        @Param("userId") UUID userId,
                        @Param("start") LocalDate start,
                        @Param("end") LocalDate end,
                        @Param("types") List<String> types);

        @Query("""
                        SELECT t FROM Transaction t
                        WHERE t.plaidAccountId IN :accountIds
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
                        WHERE t.plaidAccountId IN :accountIds
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
                        WHERE t.plaidAccountId IN :accountIds
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
                        WHERE t.plaidAccountId IN :accountIds
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
    @Query("SELECT SUM(t.amount) FROM Transaction t")
    Double sumAllAmounts();
}
