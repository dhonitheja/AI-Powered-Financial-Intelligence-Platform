package com.example.financial.autopay.scheduler;

import com.example.financial.autopay.model.entity.AutoPayReminder;
import com.example.financial.autopay.model.entity.AutoPaySchedule;
import com.example.financial.autopay.model.entity.ReminderType;
import com.example.financial.autopay.repository.AutoPayReminderRepository;
import com.example.financial.autopay.repository.AutoPayScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Daily job that detects upcoming payments and creates in-app reminders.
 *
 * <p>
 * Security note: This scheduler NEVER logs account numbers, routing numbers,
 * payment amounts, or any PII. Logs contain only schedule IDs and counts.
 *
 * <p>
 * Idempotent: uses
 * {@link AutoPayReminderRepository#existsByScheduleIdAndReminderDate}
 * to avoid duplicate reminders if the job is run multiple times per day.
 */
@Component
public class AutoPayReminderJob {

    private static final Logger log = LoggerFactory.getLogger(AutoPayReminderJob.class);

    private final AutoPayScheduleRepository scheduleRepo;
    private final AutoPayReminderRepository reminderRepo;

    public AutoPayReminderJob(AutoPayScheduleRepository scheduleRepo,
            AutoPayReminderRepository reminderRepo) {
        this.scheduleRepo = scheduleRepo;
        this.reminderRepo = reminderRepo;
    }

    /**
     * Runs daily at 8:00 AM (configurable via {@code autopay.reminder.cron}).
     * Finds all schedules whose due date falls within their individual
     * {@code reminderDaysBefore} window and creates in-app reminders.
     */
    @Scheduled(cron = "${autopay.reminder.cron:0 0 8 * * ?}")
    @Transactional
    public void processReminders() {
        LocalDate today = LocalDate.now();
        log.info("[AutoPayReminderJob] Running reminder sweep for date={}", today);

        int created = 0;
        int skipped = 0;

        // Scan schedules due in the next 30 days (max reminder window)
        for (int daysAhead = 1; daysAhead <= 30; daysAhead++) {
            LocalDate targetDate = today.plusDays(daysAhead);
            List<AutoPaySchedule> schedulesDue = scheduleRepo.findSchedulesDueOn(targetDate);

            for (AutoPaySchedule schedule : schedulesDue) {
                // Only remind if within the schedule's individual reminder window
                if (daysAhead > schedule.getReminderDaysBefore())
                    continue;

                LocalDate reminderDate = today; // remind today for this schedule

                // Idempotency check — skip if already created today
                if (reminderRepo.existsByScheduleIdAndReminderDate(schedule.getId(), reminderDate)) {
                    skipped++;
                    continue;
                }

                AutoPayReminder reminder = AutoPayReminder.of(
                        schedule, schedule.getUser(), reminderDate, ReminderType.IN_APP);
                reminderRepo.save(reminder);
                created++;
                // Log schedule ID only — no PII, no amounts, no account numbers
                log.debug("[AutoPayReminderJob] Reminder created: scheduleId={}", schedule.getId());
            }
        }

        log.info("[AutoPayReminderJob] Reminder sweep complete: created={} skipped={}", created, skipped);
    }
}
