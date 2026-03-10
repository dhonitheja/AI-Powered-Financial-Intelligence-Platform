package com.example.financial.autopay.repository;

import com.example.financial.autopay.model.entity.AutoPaySchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * All queries are parameterised through JPA — zero raw string concatenation.
 * IDOR prevention: every query is scoped to {@code userId}.
 */
@Repository
public interface AutoPayScheduleRepository extends JpaRepository<AutoPaySchedule, UUID> {

        /** Paginated list of active schedules owned by the user. */
        Page<AutoPaySchedule> findByUserIdAndActiveTrue(UUID userId, Pageable pageable);

        /** All schedules (active + inactive) owned by the user — admin-level view. */
        Page<AutoPaySchedule> findByUserId(UUID userId, Pageable pageable);

        /** Ownership-validated single schedule lookup. */
        Optional<AutoPaySchedule> findByIdAndUserId(UUID id, UUID userId);

        /** Payments due within [today, cutoffDate] for a specific user. */
        @Query("""
                        SELECT s FROM AutoPaySchedule s
                        WHERE s.user.id = :userId
                          AND s.active = true
                          AND s.nextDueDate BETWEEN :today AND :cutoffDate
                        ORDER BY s.nextDueDate ASC
                        """)
        List<AutoPaySchedule> findUpcoming(
                        @Param("userId") UUID userId,
                        @Param("today") LocalDate today,
                        @Param("cutoffDate") LocalDate cutoffDate);

        /** Overdue payments (next_due_date < today) for a specific user. */
        @Query("""
                        SELECT s FROM AutoPaySchedule s
                        WHERE s.user.id = :userId
                          AND s.active = true
                          AND s.nextDueDate < :today
                        ORDER BY s.nextDueDate ASC
                        """)
        List<AutoPaySchedule> findOverdue(
                        @Param("userId") UUID userId,
                        @Param("today") LocalDate today);

        /** Schedules needing reminders sent today for the reminder job. */
        @Query("""
                        SELECT s FROM AutoPaySchedule s
                        WHERE s.active = true
                          AND s.nextDueDate = :targetDate
                        """)
        List<AutoPaySchedule> findSchedulesDueOn(@Param("targetDate") LocalDate targetDate);

        /** Count of active schedules owned by the user (for health score). */
        long countByUserIdAndActiveTrue(UUID userId);

        /** Count active schedules by category for dashboard stats. */
        @Query("""
                        SELECT s.paymentCategory, COUNT(s), SUM(s.amount)
                        FROM AutoPaySchedule s
                        WHERE s.user.id = :userId
                          AND s.active = true
                        GROUP BY s.paymentCategory
                        """)
        List<Object[]> aggregateByCategory(@Param("userId") UUID userId);

        /**
         * Deactivate all schedules for a user (e.g., when their Stripe customer is
         * deleted).
         */
        @org.springframework.data.jpa.repository.Modifying
        @Query("UPDATE AutoPaySchedule s SET s.active = false WHERE s.user.id = :userId AND s.active = true")
        int deactivateAllForUser(@Param("userId") UUID userId);
}
