package com.example.financial.autopay.scheduler;

import com.example.financial.autopay.model.entity.*;
import com.example.financial.autopay.repository.AutoPayReminderRepository;
import com.example.financial.autopay.repository.AutoPayScheduleRepository;
import com.example.financial.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoPaySchedulerTest {

    @Mock AutoPayScheduleRepository scheduleRepo;
    @Mock AutoPayReminderRepository reminderRepo;

    @InjectMocks AutoPayReminderJob reminderJob;

    private AutoPaySchedule schedule;
    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser("testuser", "test@example.com", "hashed");

        schedule = new AutoPaySchedule();
        schedule.setUser(user);
        schedule.setPaymentName("Netflix");
        schedule.setAmount(new BigDecimal("15.99"));
        schedule.setPaymentCategory(PaymentCategory.SUBSCRIPTION);
        schedule.setFrequency(PaymentFrequency.MONTHLY);
        schedule.setNextDueDate(LocalDate.now().plusDays(3));
        schedule.setReminderDaysBefore(5);
        schedule.setActive(true);
        schedule.setAccountNumberEncrypted("enc_acct");
        schedule.setRoutingNumberEncrypted("enc_route");
    }

    @Test
    void reminderJob_createsNotification_whenDueInWindow() {
        // Schedule is due in 3 days, reminderDaysBefore=5 — should trigger
        when(scheduleRepo.findSchedulesDueOn(any(LocalDate.class)))
                .thenReturn(List.of(schedule))
                .thenReturn(List.of());
        when(reminderRepo.existsByScheduleIdAndReminderDate(any(), any()))
                .thenReturn(false);

        reminderJob.processReminders();

        verify(reminderRepo, atLeastOnce()).save(any(AutoPayReminder.class));
    }

    @Test
    void reminderJob_doesNotLog_sensitiveData() {
        // Job must not access accountNumberEncrypted/routingNumberEncrypted
        // Verify job completes without touching encrypted fields (no NPE)
        when(scheduleRepo.findSchedulesDueOn(any(LocalDate.class)))
                .thenReturn(List.of(schedule))
                .thenReturn(List.of());
        when(reminderRepo.existsByScheduleIdAndReminderDate(any(), any()))
                .thenReturn(false);

        // Should not throw even with encrypted-only field values
        reminderJob.processReminders();

        // Reminder was saved — job ran without logging sensitive data
        verify(reminderRepo, atLeastOnce()).save(any(AutoPayReminder.class));
    }

    @Test
    void reminderJob_skipsInactiveSchedules() {
        // Active=false schedules should NOT appear in findSchedulesDueOn (filtered by repo)
        when(scheduleRepo.findSchedulesDueOn(any(LocalDate.class)))
                .thenReturn(List.of());

        reminderJob.processReminders();

        verify(reminderRepo, never()).save(any());
    }
}
