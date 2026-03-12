package com.example.financial.autopay.service;

import com.example.financial.autopay.model.dto.request.CreateAutoPayRequest;
import com.example.financial.autopay.model.dto.response.AutoPayScheduleResponse;
import com.example.financial.autopay.model.entity.*;
import com.example.financial.autopay.repository.AutoPayExecutionLogRepository;
import com.example.financial.autopay.repository.AutoPayReminderRepository;
import com.example.financial.autopay.repository.AutoPayScheduleRepository;
import com.example.financial.entity.AppUser;
import com.example.financial.repository.AppUserRepository;
import com.example.financial.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutoPayServiceTest {

    @Mock AutoPayScheduleRepository scheduleRepo;
    @Mock AutoPayReminderRepository reminderRepo;
    @Mock AutoPayExecutionLogRepository logRepo;
    @Mock AppUserRepository userRepo;
    @Mock EncryptionService encryptionService;
    @Mock WealthixAiClient aiClient;

    @InjectMocks AutoPayService autoPayService;

    private AppUser user;
    private CreateAutoPayRequest request;

    @BeforeEach
    void setUp() {
        user = new AppUser("testuser", "test@example.com", "hashed");

        request = new CreateAutoPayRequest();
        request.setPaymentName("Netflix");
        request.setPaymentCategory(PaymentCategory.SUBSCRIPTION);
        request.setAmount(new BigDecimal("15.99"));
        request.setFrequency(PaymentFrequency.MONTHLY);
        request.setNextDueDate(LocalDate.now().plusDays(10));
        request.setAccountNumber("1234567890");
        request.setRoutingNumber("021000021");
        request.setNotes("personal streaming");

        when(userRepo.findFirstByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(user));
    }

    @Test
    void createSchedule_encryptsAccountNumber() {
        when(encryptionService.encrypt("1234567890")).thenReturn("enc_account");
        when(encryptionService.encrypt("021000021")).thenReturn("enc_routing");
        when(encryptionService.encrypt("personal streaming")).thenReturn("enc_notes");
        when(scheduleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        autoPayService.create("test@example.com", request);

        ArgumentCaptor<AutoPaySchedule> captor = ArgumentCaptor.forClass(AutoPaySchedule.class);
        verify(scheduleRepo).save(captor.capture());
        AutoPaySchedule saved = captor.getValue();

        assertThat(saved.getAccountNumberEncrypted()).isEqualTo("enc_account");
        assertThat(saved.getAccountNumberEncrypted()).doesNotContain("1234567890");
    }

    @Test
    void createSchedule_encryptsRoutingNumber() {
        when(encryptionService.encrypt("1234567890")).thenReturn("enc_account");
        when(encryptionService.encrypt("021000021")).thenReturn("enc_routing");
        when(encryptionService.encrypt("personal streaming")).thenReturn("enc_notes");
        when(scheduleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        autoPayService.create("test@example.com", request);

        ArgumentCaptor<AutoPaySchedule> captor = ArgumentCaptor.forClass(AutoPaySchedule.class);
        verify(scheduleRepo).save(captor.capture());
        assertThat(captor.getValue().getRoutingNumberEncrypted()).doesNotContain("021000021");
    }

    @Test
    void createSchedule_encryptsNotes() {
        when(encryptionService.encrypt(any())).thenReturn("enc_value");
        when(scheduleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        autoPayService.create("test@example.com", request);

        ArgumentCaptor<AutoPaySchedule> captor = ArgumentCaptor.forClass(AutoPaySchedule.class);
        verify(scheduleRepo).save(captor.capture());
        assertThat(captor.getValue().getNotesEncrypted()).doesNotContain("personal streaming");
    }

    @Test
    void getSchedule_masksAccountNumber() {
        AutoPaySchedule schedule = buildSchedule(user);
        when(scheduleRepo.findByIdAndUserId(eq(schedule.getId()), any()))
                .thenReturn(Optional.of(schedule));
        when(encryptionService.decrypt("enc_account")).thenReturn("1234567890");

        AutoPayScheduleResponse response = autoPayService.getById("test@example.com", schedule.getId());

        assertThat(response.accountNumberMasked()).contains("****");
        assertThat(response.accountNumberMasked()).doesNotContain("1234567890");
        assertThat(response.accountNumberMasked()).endsWith("7890");
    }

    @Test
    void getSchedule_throwsForbidden_whenWrongUser() {
        UUID scheduleId = UUID.randomUUID();
        when(scheduleRepo.findByIdAndUserId(eq(scheduleId), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                autoPayService.getById("test@example.com", scheduleId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void deleteSchedule_softDeletes_notHardDeletes() {
        AutoPaySchedule schedule = buildSchedule(user);
        when(scheduleRepo.findByIdAndUserId(eq(schedule.getId()), any()))
                .thenReturn(Optional.of(schedule));
        when(scheduleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        autoPayService.softDelete("test@example.com", schedule.getId());

        ArgumentCaptor<AutoPaySchedule> captor = ArgumentCaptor.forClass(AutoPaySchedule.class);
        verify(scheduleRepo).save(captor.capture());
        verify(scheduleRepo, never()).delete(any());
        verify(scheduleRepo, never()).deleteById(any());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    void toggleSchedule_flipsIsActive() {
        AutoPaySchedule schedule = buildSchedule(user);
        schedule.setActive(true);
        when(scheduleRepo.findByIdAndUserId(eq(schedule.getId()), any()))
                .thenReturn(Optional.of(schedule));
        when(scheduleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        autoPayService.toggleActive("test@example.com", schedule.getId());

        ArgumentCaptor<AutoPaySchedule> captor = ArgumentCaptor.forClass(AutoPaySchedule.class);
        verify(scheduleRepo).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    void calculateNextDueDate_monthly() {
        LocalDate base = LocalDate.of(2025, 1, 15);
        LocalDate next = autoPayService.calculateNextDueDate(PaymentFrequency.MONTHLY, base);
        assertThat(next).isEqualTo(LocalDate.of(2025, 2, 15));
    }

    @Test
    void calculateNextDueDate_weekly() {
        LocalDate base = LocalDate.of(2025, 1, 1);
        LocalDate next = autoPayService.calculateNextDueDate(PaymentFrequency.WEEKLY, base);
        assertThat(next).isEqualTo(LocalDate.of(2025, 1, 8));
    }

    @Test
    void calculateNextDueDate_annually() {
        LocalDate base = LocalDate.of(2025, 3, 10);
        LocalDate next = autoPayService.calculateNextDueDate(PaymentFrequency.ANNUALLY, base);
        assertThat(next).isEqualTo(LocalDate.of(2026, 3, 10));
    }

    @Test
    void getMonthlyEquivalent_quarterly() {
        BigDecimal quarterly = new BigDecimal("300.00");
        BigDecimal monthly = autoPayService.getMonthlyEquivalent(quarterly, PaymentFrequency.QUARTERLY);
        assertThat(monthly).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void calculatePaymentHealthScore_returnsZeroToHundred() {
        when(logRepo.countByUserIdAndStatus(any(), any())).thenReturn(0L);
        when(scheduleRepo.countByUserIdAndActiveTrue(any())).thenReturn(0L);

        int score = autoPayService.calculatePaymentHealthScore(UUID.randomUUID());
        assertThat(score).isBetween(0, 100);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private AutoPaySchedule buildSchedule(AppUser owner) {
        AutoPaySchedule s = new AutoPaySchedule();
        s.setUser(owner);
        s.setPaymentName("Netflix");
        s.setPaymentCategory(PaymentCategory.SUBSCRIPTION);
        s.setAmount(new BigDecimal("15.99"));
        s.setFrequency(PaymentFrequency.MONTHLY);
        s.setNextDueDate(LocalDate.now().plusDays(5));
        s.setAccountNumberEncrypted("enc_account");
        s.setRoutingNumberEncrypted("enc_routing");
        s.setActive(true);
        return s;
    }
}
