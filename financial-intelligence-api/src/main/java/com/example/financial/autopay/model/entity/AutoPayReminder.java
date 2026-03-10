package com.example.financial.autopay.model.entity;

import com.example.financial.entity.AppUser;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Scheduled reminder for an autopay payment.
 */
@Entity
@Table(name = "autopay_reminders")
public class AutoPayReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private AutoPaySchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "reminder_date", nullable = false)
    private LocalDate reminderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, columnDefinition = "reminder_type")
    private ReminderType reminderType = ReminderType.IN_APP;

    @Column(name = "is_sent", nullable = false)
    private boolean sent = false;

    @Column(name = "sent_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    protected AutoPayReminder() {
    }

    public static AutoPayReminder of(AutoPaySchedule schedule, AppUser user,
            LocalDate reminderDate, ReminderType type) {
        AutoPayReminder r = new AutoPayReminder();
        r.schedule = schedule;
        r.user = user;
        r.reminderDate = reminderDate;
        r.reminderType = type;
        return r;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public AutoPaySchedule getSchedule() {
        return schedule;
    }

    public AppUser getUser() {
        return user;
    }

    public LocalDate getReminderDate() {
        return reminderDate;
    }

    public void setReminderDate(LocalDate reminderDate) {
        this.reminderDate = reminderDate;
    }

    public ReminderType getReminderType() {
        return reminderType;
    }

    public void setReminderType(ReminderType reminderType) {
        this.reminderType = reminderType;
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
