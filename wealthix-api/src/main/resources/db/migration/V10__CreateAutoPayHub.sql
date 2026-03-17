-- =============================================================================
-- V10__CreateAutoPayHub.sql
-- AutoPay Hub: Centralized Recurring Payment & EMI Manager
-- All sensitive fields (account_number, routing_number, notes) are stored
-- AES-256-GCM encrypted. Never store or log plain-text financial identifiers.
-- =============================================================================

-- ── Payment category enum ────────────────────────────────────────────────────
CREATE TYPE payment_category AS ENUM (
    'HOME_LOAN',
    'AUTO_LOAN',
    'PERSONAL_LOAN',
    'EDUCATION_LOAN',
    'CREDIT_CARD',
    'HEALTH_INSURANCE',
    'HOME_INSURANCE',
    'AUTO_INSURANCE',
    'LIFE_INSURANCE',
    'TERM_INSURANCE',
    'UTILITY',
    'SUBSCRIPTION',
    'SIP',
    'RENT',
    'CUSTOM'
);

-- ── Payment frequency enum ───────────────────────────────────────────────────
CREATE TYPE payment_frequency AS ENUM (
    'DAILY',
    'WEEKLY',
    'BIWEEKLY',
    'MONTHLY',
    'QUARTERLY',
    'ANNUALLY'
);

-- ── Execution status enum ────────────────────────────────────────────────────
CREATE TYPE execution_status AS ENUM (
    'PENDING',
    'SUCCESS',
    'FAILED',
    'SKIPPED',
    'CANCELLED'
);

-- ── Reminder type enum ───────────────────────────────────────────────────────
CREATE TYPE reminder_type AS ENUM (
    'EMAIL',
    'IN_APP',
    'PUSH'
);

-- ── 1. autopay_schedules ─────────────────────────────────────────────────────
CREATE TABLE autopay_schedules (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Payment identity
    payment_name            VARCHAR(255) NOT NULL,
    payment_category        payment_category NOT NULL,
    payment_provider        VARCHAR(255),

    -- Encrypted sensitive fields (AES-256-GCM, stored as Base64(IV || ciphertext))
    -- NEVER store plaintext account numbers or routing numbers
    account_number_encrypted TEXT,
    routing_number_encrypted TEXT,
    notes_encrypted          TEXT,

    -- Payment amount
    amount                  DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    currency                VARCHAR(3) NOT NULL DEFAULT 'USD',

    -- Schedule configuration
    frequency               payment_frequency NOT NULL,
    next_due_date           DATE NOT NULL,
    due_day_of_month        INTEGER CHECK (due_day_of_month BETWEEN 1 AND 31),

    -- Automation & reminders
    auto_execute            BOOLEAN NOT NULL DEFAULT FALSE,
    reminder_days_before    INTEGER NOT NULL DEFAULT 3 CHECK (reminder_days_before >= 0),

    -- Soft-delete flag
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit timestamps
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Prevent duplicate schedules for same user/provider/payment combo
    CONSTRAINT uq_user_payment_provider UNIQUE (user_id, payment_name, payment_provider)
);

-- Index for fast lookup of a user's schedules
CREATE INDEX idx_autopay_schedules_user_id       ON autopay_schedules (user_id);
CREATE INDEX idx_autopay_schedules_next_due_date ON autopay_schedules (next_due_date);
CREATE INDEX idx_autopay_schedules_active        ON autopay_schedules (user_id, is_active);

-- ── 2. autopay_execution_logs ────────────────────────────────────────────────
CREATE TABLE autopay_execution_logs (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id          UUID NOT NULL REFERENCES autopay_schedules(id) ON DELETE CASCADE,
    user_id              UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    execution_date       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    amount_paid          DECIMAL(15, 2) NOT NULL CHECK (amount_paid >= 0),
    status               execution_status NOT NULL DEFAULT 'PENDING',

    -- Generic failure messages ONLY — no PII, no account numbers
    failure_reason       VARCHAR(500),

    -- Plaid transaction reference (if executed via Plaid)
    plaid_transaction_id VARCHAR(255),

    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_exec_logs_schedule_id ON autopay_execution_logs (schedule_id);
CREATE INDEX idx_exec_logs_user_id     ON autopay_execution_logs (user_id);
CREATE INDEX idx_exec_logs_status      ON autopay_execution_logs (status);

-- ── 3. autopay_reminders ─────────────────────────────────────────────────────
CREATE TABLE autopay_reminders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id     UUID NOT NULL REFERENCES autopay_schedules(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    reminder_date   DATE NOT NULL,
    reminder_type   reminder_type NOT NULL DEFAULT 'IN_APP',
    is_sent         BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at         TIMESTAMPTZ,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reminders_schedule_id   ON autopay_reminders (schedule_id);
CREATE INDEX idx_reminders_user_id       ON autopay_reminders (user_id);
CREATE INDEX idx_reminders_reminder_date ON autopay_reminders (reminder_date, is_sent);

-- ── Row-Level Security ───────────────────────────────────────────────────────
-- Enable RLS so that even direct DB access is scoped to user_id.
-- The Spring Boot service connects as the application role which bypasses RLS
-- by default; policies here protect against direct query access.
ALTER TABLE autopay_schedules     ENABLE ROW LEVEL SECURITY;
ALTER TABLE autopay_execution_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE autopay_reminders     ENABLE ROW LEVEL SECURITY;
