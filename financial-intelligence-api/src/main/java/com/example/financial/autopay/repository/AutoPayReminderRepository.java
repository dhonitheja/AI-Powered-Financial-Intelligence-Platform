package com.example.financial.autopay.repository;

import com.example.financial.autopay.model.entity.AutoPayReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AutoPayReminderRepository extends JpaRepository<AutoPayReminder, UUID> {

    /**
     * Find unsent reminders due on a given date (used by the nightly scheduler).
     */
    @Query("""
            SELECT r FROM AutoPayReminder r
            WHERE r.reminderDate = :date
              AND r.sent = false
            """)
    List<AutoPayReminder> findUnsentDueOn(@Param("date") LocalDate date);

    /**
     * Check if a reminder already exists for this schedule + date combo
     * (idempotency).
     */
    boolean existsByScheduleIdAndReminderDate(UUID scheduleId, LocalDate reminderDate);
}
