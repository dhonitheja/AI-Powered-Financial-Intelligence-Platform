package com.example.financial.autopay.service;

import com.example.financial.autopay.model.dto.request.CreateAutoPayRequest;
import com.example.financial.autopay.model.dto.request.UpdateAutoPayRequest;
import com.example.financial.autopay.model.dto.response.AutoPayDashboardResponse;
import com.example.financial.autopay.model.dto.response.AutoPayExecutionLogResponse;
import com.example.financial.autopay.model.dto.response.AutoPayScheduleResponse;
import com.example.financial.autopay.model.entity.*;
import com.example.financial.autopay.repository.AutoPayExecutionLogRepository;
import com.example.financial.autopay.repository.AutoPayReminderRepository;
import com.example.financial.autopay.repository.AutoPayScheduleRepository;
import com.example.financial.entity.AppUser;
import com.example.financial.repository.AppUserRepository;
import com.example.financial.service.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core business logic for the AutoPay Hub.
 *
 * <p>
 * Security contract:
 * <ul>
 * <li>All account/routing numbers are encrypted via {@link EncryptionService}
 * before any persistence call.
 * <li>API responses use {@link #buildResponse} which decrypts the last-4 only
 * and returns a masked string — never the full number.
 * <li>Every query is ownership-validated against the authenticated userId.
 * <li>Sensitive fields are NEVER logged (Slf4j MDC contains only schedule IDs
 * and user IDs).
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class AutoPayService {

    private static final Logger log = LoggerFactory.getLogger(AutoPayService.class);

    private final AutoPayScheduleRepository scheduleRepo;
    private final AutoPayExecutionLogRepository logRepo;
    private final AutoPayReminderRepository reminderRepo;
    private final AppUserRepository userRepo;
    private final EncryptionService encryptionService;
    private final WealthixAiClient aiClient;

    public AutoPayService(AutoPayScheduleRepository scheduleRepo,
            AutoPayExecutionLogRepository logRepo,
            AutoPayReminderRepository reminderRepo,
            AppUserRepository userRepo,
            EncryptionService encryptionService,
            WealthixAiClient aiClient) {
        this.scheduleRepo = scheduleRepo;
        this.logRepo = logRepo;
        this.reminderRepo = reminderRepo;
        this.userRepo = userRepo;
        this.encryptionService = encryptionService;
        this.aiClient = aiClient;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "dashboard", key = "#userEmail")
    public AutoPayScheduleResponse create(String userEmail, CreateAutoPayRequest req) {
        AppUser user = resolveUser(userEmail);

        AutoPaySchedule schedule = new AutoPaySchedule();
        schedule.setUser(user);
        schedule.setPaymentName(req.getPaymentName());
        schedule.setPaymentCategory(req.getPaymentCategory());
        schedule.setPaymentProvider(req.getPaymentProvider());
        schedule.setAmount(req.getAmount());
        schedule.setCurrency(req.getCurrency() != null ? req.getCurrency() : "USD");
        schedule.setFrequency(req.getFrequency());
        schedule.setNextDueDate(req.getNextDueDate());
        schedule.setDueDayOfMonth(req.getDueDayOfMonth());
        schedule.setAutoExecute(req.isAutoExecute());
        schedule.setReminderDaysBefore(req.getReminderDaysBefore());

        // ── Encrypt sensitive fields — NEVER persist plaintext ────────────────
        if (req.getAccountNumber() != null && !req.getAccountNumber().isBlank()) {
            schedule.setAccountNumberEncrypted(encryptionService.encrypt(req.getAccountNumber()));
        }
        if (req.getRoutingNumber() != null && !req.getRoutingNumber().isBlank()) {
            schedule.setRoutingNumberEncrypted(encryptionService.encrypt(req.getRoutingNumber()));
        }
        if (req.getNotes() != null && !req.getNotes().isBlank()) {
            schedule.setNotesEncrypted(encryptionService.encrypt(req.getNotes()));
        }

        AutoPaySchedule saved = scheduleRepo.save(schedule);

        // ── Phase 2B: Call AI categorization if CUSTOM ────────────────────────
        if (saved.getPaymentCategory() == PaymentCategory.CUSTOM) {
            aiClient.categorizePayment(saved.getPaymentName())
                    .ifPresent(cat -> {
                        // In Phase 3 we would map this to ai_suggested_category
                        // in the response if a dedicated field existed.
                        // Currently we just log the intelligent finding.
                        log.info("[AutoPay] AI suggested category '{}' for schedule '{}'",
                                cat, saved.getId());
                    });
        }

        // Audit log — note: no sensitive fields logged
        log.info("[AutoPay] Schedule created: id={} userId={} category={}",
                saved.getId(), user.getId(), saved.getPaymentCategory());

        return buildResponse(saved);
    }

    public Page<AutoPayScheduleResponse> listSchedules(String userEmail, Pageable pageable) {
        UUID userId = resolveUserId(userEmail);
        return scheduleRepo.findByUserIdAndActiveTrue(userId, pageable)
                .map(this::buildResponse);
    }

    public AutoPayScheduleResponse getById(String userEmail, UUID scheduleId) {
        UUID userId = resolveUserId(userEmail);
        AutoPaySchedule schedule = scheduleRepo.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Schedule not found or access denied"));
        return buildResponse(schedule);
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "#userEmail")
    public AutoPayScheduleResponse update(String userEmail, UUID scheduleId, UpdateAutoPayRequest req) {
        UUID userId = resolveUserId(userEmail);
        AutoPaySchedule schedule = scheduleRepo.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Schedule not found or access denied"));

        // Apply non-null updates (PATCH semantics)
        if (req.getPaymentName() != null)
            schedule.setPaymentName(req.getPaymentName());
        if (req.getPaymentProvider() != null)
            schedule.setPaymentProvider(req.getPaymentProvider());
        if (req.getAmount() != null)
            schedule.setAmount(req.getAmount());
        if (req.getCurrency() != null)
            schedule.setCurrency(req.getCurrency());
        if (req.getFrequency() != null)
            schedule.setFrequency(req.getFrequency());
        if (req.getNextDueDate() != null)
            schedule.setNextDueDate(req.getNextDueDate());
        if (req.getDueDayOfMonth() != null)
            schedule.setDueDayOfMonth(req.getDueDayOfMonth());
        if (req.getAutoExecute() != null)
            schedule.setAutoExecute(req.getAutoExecute());
        if (req.getReminderDaysBefore() != null)
            schedule.setReminderDaysBefore(req.getReminderDaysBefore());

        // Re-encrypt if provided
        if (req.getAccountNumber() != null && !req.getAccountNumber().isBlank()) {
            schedule.setAccountNumberEncrypted(encryptionService.encrypt(req.getAccountNumber()));
        }
        if (req.getRoutingNumber() != null && !req.getRoutingNumber().isBlank()) {
            schedule.setRoutingNumberEncrypted(encryptionService.encrypt(req.getRoutingNumber()));
        }
        if (req.getNotes() != null) {
            schedule.setNotesEncrypted(req.getNotes().isBlank()
                    ? null
                    : encryptionService.encrypt(req.getNotes()));
        }

        AutoPaySchedule saved = scheduleRepo.save(schedule);
        log.info("[AutoPay] Schedule updated: id={} userId={}", saved.getId(), userId);
        return buildResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "#userEmail")
    public void softDelete(String userEmail, UUID scheduleId) {
        UUID userId = resolveUserId(userEmail);
        AutoPaySchedule schedule = scheduleRepo.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Schedule not found or access denied"));
        schedule.setActive(false);
        scheduleRepo.save(schedule);
        log.info("[AutoPay] Schedule soft-deleted: id={} userId={}", scheduleId, userId);
    }

    @Transactional
    @CacheEvict(value = "dashboard", key = "#userEmail")
    public AutoPayScheduleResponse toggleActive(String userEmail, UUID scheduleId) {
        UUID userId = resolveUserId(userEmail);
        AutoPaySchedule schedule = scheduleRepo.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Schedule not found or access denied"));
        schedule.setActive(!schedule.isActive());
        AutoPaySchedule saved = scheduleRepo.save(schedule);
        log.info("[AutoPay] Schedule toggled: id={} active={} userId={}", scheduleId, saved.isActive(), userId);
        return buildResponse(saved);
    }

    // ── Upcoming / Overdue ────────────────────────────────────────────────────

    public List<AutoPayScheduleResponse> getUpcoming(String userEmail, int days) {
        UUID userId = resolveUserId(userEmail);
        LocalDate today = LocalDate.now();
        return scheduleRepo.findUpcoming(userId, today, today.plusDays(days))
                .stream().map(this::buildResponse).toList();
    }

    public List<AutoPayScheduleResponse> getOverdue(String userEmail) {
        UUID userId = resolveUserId(userEmail);
        return scheduleRepo.findOverdue(userId, LocalDate.now())
                .stream().map(this::buildResponse).toList();
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @Cacheable(value = "dashboard", key = "#userEmail", unless = "#result == null")
    public AutoPayDashboardResponse getDashboard(String userEmail) {
        UUID userId = resolveUserId(userEmail);
        LocalDate today = LocalDate.now();

        List<Object[]> aggregates = scheduleRepo.aggregateByCategory(userId);
        Map<String, AutoPayDashboardResponse.CategoryStat> categoryBreakdown = new LinkedHashMap<>();
        BigDecimal totalMonthly = BigDecimal.ZERO;

        for (Object[] row : aggregates) {
            PaymentCategory cat = (PaymentCategory) row[0];
            long count = (Long) row[1];
            BigDecimal sum = (BigDecimal) row[2];
            // Normalize sum to monthly using the dominant frequency (approximation)
            categoryBreakdown.put(cat.name(),
                    new AutoPayDashboardResponse.CategoryStat((int) count, sum));
            totalMonthly = totalMonthly.add(sum);
        }

        List<AutoPayScheduleResponse> upcoming = scheduleRepo
                .findUpcoming(userId, today, today.plusDays(30))
                .stream().map(this::buildResponse).toList();

        int dueSoon = (int) upcoming.stream()
                .filter(s -> !LocalDate.parse(s.nextDueDate().toString()).isAfter(today.plusDays(7)))
                .count();
        int overdue = scheduleRepo.findOverdue(userId, today).size();

        int activeCount = (int) aggregates.stream().mapToLong(row -> (Long) row[1]).sum();
        int healthScore = calculatePaymentHealthScore(userId);

        return new AutoPayDashboardResponse(
                totalMonthly,
                totalMonthly.multiply(BigDecimal.valueOf(12)),
                activeCount,
                dueSoon,
                overdue,
                categoryBreakdown,
                upcoming,
                healthScore,
                healthLabel(healthScore));
    }

    // ── Execution ─────────────────────────────────────────────────────────────

    @Transactional
    public AutoPayExecutionLogResponse triggerManualExecution(String userEmail, UUID scheduleId) {
        UUID userId = resolveUserId(userEmail);
        AppUser user = resolveUser(userEmail);
        AutoPaySchedule schedule = scheduleRepo.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Schedule not found or access denied"));

        AutoPayExecutionLog entry = AutoPayExecutionLog.pending(schedule, user, schedule.getAmount());
        // In a real implementation, this would call a payment gateway/Plaid.
        // For now, mark as pending and let downstream processing handle it.
        entry.setStatus(ExecutionStatus.PENDING);
        AutoPayExecutionLog saved = logRepo.save(entry);

        // Advance next due date after successful scheduling
        schedule.setNextDueDate(calculateNextDueDate(schedule.getFrequency(), schedule.getNextDueDate()));
        scheduleRepo.save(schedule);

        log.info("[AutoPay] Manual execution triggered: scheduleId={} userId={}", scheduleId, userId);
        return mapLog(saved);
    }

    // ── Execution Logs ────────────────────────────────────────────────────────

    public Page<AutoPayExecutionLogResponse> getExecutionLogs(String userEmail, Pageable pageable) {
        UUID userId = resolveUserId(userEmail);
        return logRepo.findByUserIdOrderByExecutionDateDesc(userId, pageable).map(this::mapLog);
    }

    public Page<AutoPayExecutionLogResponse> getExecutionLogsForSchedule(
            String userEmail, UUID scheduleId, Pageable pageable) {
        UUID userId = resolveUserId(userEmail);
        // Ownership verified inside query via user.id = :userId
        return logRepo.findByScheduleAndUser(scheduleId, userId, pageable).map(this::mapLog);
    }

    // ── Category Metadata ─────────────────────────────────────────────────────

    public List<Map<String, Object>> getCategoryMetadata() {
        return Arrays.stream(PaymentCategory.values()).map(cat -> {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("value", cat.name());
            meta.put("displayName", categoryDisplayName(cat));
            meta.put("icon", categoryIcon(cat));
            meta.put("color", categoryColor(cat));
            return meta;
        }).collect(Collectors.toList());
    }

    // ── Business Logic ────────────────────────────────────────────────────────

    /**
     * Calculates the next due date based on frequency from the current due date.
     */
    public LocalDate calculateNextDueDate(PaymentFrequency frequency, LocalDate currentDueDate) {
        return switch (frequency) {
            case DAILY -> currentDueDate.plusDays(1);
            case WEEKLY -> currentDueDate.plusWeeks(1);
            case BIWEEKLY -> currentDueDate.plusWeeks(2);
            case MONTHLY -> currentDueDate.plusMonths(1);
            case QUARTERLY -> currentDueDate.plusMonths(3);
            case ANNUALLY -> currentDueDate.plusYears(1);
        };
    }

    /**
     * Normalises an amount to its monthly equivalent for dashboard aggregation.
     */
    public BigDecimal getMonthlyEquivalent(BigDecimal amount, PaymentFrequency frequency) {
        return switch (frequency) {
            case DAILY -> amount.multiply(BigDecimal.valueOf(30));
            case WEEKLY -> amount.multiply(BigDecimal.valueOf(52))
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            case BIWEEKLY -> amount.multiply(BigDecimal.valueOf(26))
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            case MONTHLY -> amount;
            case QUARTERLY -> amount.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            case ANNUALLY -> amount.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        };
    }

    /**
     * Calculates a 0–100 payment health score:
     * 40 pts — on-time payment rate (successful / total attempts)
     * 30 pts — active coverage (having ≥ 5 active schedules = full points, scales
     * down)
     * 30 pts — no overdue items (each overdue item deducts 10 pts, floor 0)
     */
    public int calculatePaymentHealthScore(UUID userId) {
        long total = logRepo.countByUserIdAndStatus(userId, ExecutionStatus.SUCCESS)
                + logRepo.countByUserIdAndStatus(userId, ExecutionStatus.FAILED);
        long success = logRepo.countByUserIdAndStatus(userId, ExecutionStatus.SUCCESS);

        int onTimeScore = (total == 0) ? 30 : (int) Math.round((double) success / total * 40);
        int coverageScore = (int) Math.min(30L, scheduleRepo.countByUserIdAndActiveTrue(userId) / 5 * 6);
        int overdueCount = scheduleRepo.findOverdue(userId, LocalDate.now()).size();
        int overdueScore = Math.max(0, 30 - overdueCount * 10);

        return Math.min(100, onTimeScore + coverageScore + overdueScore);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Maps an entity to a safe API response.
     * Decrypts the last 4 digits of account number for masking — never returns full
     * number.
     */
    private AutoPayScheduleResponse buildResponse(AutoPaySchedule s) {
        String masked = maskAccountNumber(s.getAccountNumberEncrypted());
        boolean hasRouting = s.getRoutingNumberEncrypted() != null;
        boolean hasNotes = s.getNotesEncrypted() != null;
        BigDecimal monthly = getMonthlyEquivalent(s.getAmount(), s.getFrequency());
        String status = deriveStatus(s);

        return new AutoPayScheduleResponse(
                s.getId(),
                s.getPaymentName(),
                s.getPaymentCategory(),
                categoryDisplayName(s.getPaymentCategory()),
                s.getPaymentProvider(),
                masked,
                hasRouting,
                hasNotes,
                s.getAmount(),
                s.getCurrency(),
                monthly,
                s.getFrequency(),
                s.getNextDueDate(),
                s.getDueDayOfMonth(),
                s.isAutoExecute(),
                s.getReminderDaysBefore(),
                s.isActive(),
                status,
                s.getCreatedAt(),
                s.getUpdatedAt());
    }

    /**
     * Returns "****XXXX" (last 4 digits) or null if no account is stored.
     * NEVER returns full account number.
     */
    private String maskAccountNumber(String encrypted) {
        if (encrypted == null)
            return null;
        try {
            String plain = encryptionService.decrypt(encrypted);
            if (plain.length() < 4)
                return "****";
            return "****" + plain.substring(plain.length() - 4);
        } catch (Exception e) {
            log.warn("[AutoPay] Could not decrypt account number for masking — returning placeholder");
            return "****";
        }
    }

    private String deriveStatus(AutoPaySchedule s) {
        if (!s.isActive())
            return "INACTIVE";
        LocalDate today = LocalDate.now();
        if (s.getNextDueDate().isBefore(today))
            return "OVERDUE";
        if (!s.getNextDueDate().isAfter(today.plusDays(7)))
            return "DUE_SOON";
        return "ACTIVE";
    }

    private AutoPayExecutionLogResponse mapLog(AutoPayExecutionLog l) {
        return new AutoPayExecutionLogResponse(
                l.getId(),
                l.getSchedule().getId(),
                l.getSchedule().getPaymentName(),
                l.getExecutionDate(),
                l.getAmountPaid(),
                l.getSchedule().getCurrency(),
                l.getStatus(),
                l.getFailureReason(),
                l.getPlaidTransactionId(),
                l.getStripePaymentIntentId(),
                l.getPlaidVerificationStatus() != null ? l.getPlaidVerificationStatus().name() : null,
                l.getPlaidMatchedTransactionId(),
                l.getCreatedAt());
    }

    private AppUser resolveUser(String email) {
        return userRepo.findFirstByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public UUID resolveUserId(String email) {
        return resolveUser(email).getId();
    }

    /**
     * Public alias for mapLog — used when the controller has a raw execution log
     * entity.
     */
    public AutoPayExecutionLogResponse toExecutionLogResponse(AutoPayExecutionLog log) {
        return mapLog(log);
    }

    private String healthLabel(int score) {
        if (score >= 80)
            return "Excellent";
        if (score >= 60)
            return "Good";
        if (score >= 40)
            return "Fair";
        return "At Risk";
    }

    private String categoryDisplayName(PaymentCategory cat) {
        return switch (cat) {
            case HOME_LOAN -> "Home Loan EMI";
            case AUTO_LOAN -> "Auto Loan EMI";
            case PERSONAL_LOAN -> "Personal Loan EMI";
            case EDUCATION_LOAN -> "Education Loan EMI";
            case CREDIT_CARD -> "Credit Card";
            case HEALTH_INSURANCE -> "Health Insurance";
            case HOME_INSURANCE -> "Home/Renters Insurance";
            case AUTO_INSURANCE -> "Auto Insurance";
            case LIFE_INSURANCE -> "Life Insurance";
            case TERM_INSURANCE -> "Term Insurance";
            case UTILITY -> "Utility Bill";
            case SUBSCRIPTION -> "Subscription";
            case SIP -> "Investment SIP";
            case RENT -> "Rent";
            case CUSTOM -> "Custom";
        };
    }

    private String categoryIcon(PaymentCategory cat) {
        return switch (cat) {
            case HOME_LOAN, HOME_INSURANCE, RENT -> "Home";
            case AUTO_LOAN, AUTO_INSURANCE -> "Car";
            case PERSONAL_LOAN, EDUCATION_LOAN -> "GraduationCap";
            case CREDIT_CARD -> "CreditCard";
            case HEALTH_INSURANCE -> "Heart";
            case LIFE_INSURANCE, TERM_INSURANCE -> "Shield";
            case UTILITY -> "Zap";
            case SUBSCRIPTION -> "Play";
            case SIP -> "TrendingUp";
            case CUSTOM -> "MoreHorizontal";
        };
    }

    private String categoryColor(PaymentCategory cat) {
        return switch (cat) {
            case HOME_LOAN -> "#6366f1";
            case AUTO_LOAN -> "#3b82f6";
            case PERSONAL_LOAN, EDUCATION_LOAN -> "#8b5cf6";
            case CREDIT_CARD -> "#f59e0b";
            case HEALTH_INSURANCE -> "#ef4444";
            case HOME_INSURANCE -> "#10b981";
            case AUTO_INSURANCE -> "#06b6d4";
            case LIFE_INSURANCE, TERM_INSURANCE -> "#14b8a6";
            case UTILITY -> "#f97316";
            case SUBSCRIPTION -> "#a855f7";
            case SIP -> "#22c55e";
            case RENT -> "#64748b";
            case CUSTOM -> "#94a3b8";
        };
    }
}
