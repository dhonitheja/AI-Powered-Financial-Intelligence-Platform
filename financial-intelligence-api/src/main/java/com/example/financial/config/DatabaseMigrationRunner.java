package com.example.financial.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Runs SQL migrations at startup that Hibernate's ddl-auto:update cannot
 * execute through the Supabase PgBouncer pooler (which blocks DDL in
 * transaction mode). This ensures all required columns and tables always exist.
 */
@Component
public class DatabaseMigrationRunner implements ApplicationRunner {

        private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

        private final JdbcTemplate jdbcTemplate;

        public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
                this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        public void run(ApplicationArguments args) {
                log.info("Running startup database migrations...");
                try {
                        jdbcTemplate.execute(
                                        "ALTER TABLE users ADD COLUMN IF NOT EXISTS two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE");
                        log.info("✔ Column 'two_factor_enabled' ensured.");

                        jdbcTemplate.execute(
                                        "ALTER TABLE users ADD COLUMN IF NOT EXISTS two_factor_secret VARCHAR(255)");
                        log.info("✔ Column 'two_factor_secret' ensured.");

                        // ── Financial account enrichment columns ───────────────────────────
                        jdbcTemplate.execute(
                                        "ALTER TABLE user_bank_connections ADD COLUMN IF NOT EXISTS account_id VARCHAR(255)");
                        jdbcTemplate.execute(
                                        "ALTER TABLE user_bank_connections ADD COLUMN IF NOT EXISTS account_name VARCHAR(255)");
                        jdbcTemplate.execute(
                                        "ALTER TABLE user_bank_connections ADD COLUMN IF NOT EXISTS account_type VARCHAR(20) DEFAULT 'CHECKING'");
                        jdbcTemplate.execute(
                                        "ALTER TABLE user_bank_connections ADD COLUMN IF NOT EXISTS current_balance DOUBLE PRECISION");
                        jdbcTemplate.execute(
                                        "ALTER TABLE user_bank_connections ADD COLUMN IF NOT EXISTS credit_limit DOUBLE PRECISION");
                        log.info("✔ Account enrichment columns ensured.");

                        // ── Transaction account-type columns ───────────────────────────────
                        jdbcTemplate.execute(
                                        "ALTER TABLE transactions ADD COLUMN IF NOT EXISTS account_id VARCHAR(255)");
                        jdbcTemplate.execute(
                                        "ALTER TABLE transactions ADD COLUMN IF NOT EXISTS account_type VARCHAR(20) DEFAULT 'CHECKING'");
                        log.info("✔ Transaction account columns ensured.");

                        // ── AutoPay Hub: create enum types (idempotent) ────────────────────
                        jdbcTemplate.execute(
                                        "DO $$ BEGIN " +
                                                        "  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_category') THEN "
                                                        +
                                                        "    CREATE TYPE payment_category AS ENUM (" +
                                                        "      'HOME_LOAN','AUTO_LOAN','PERSONAL_LOAN','EDUCATION_LOAN',"
                                                        +
                                                        "      'CREDIT_CARD','HEALTH_INSURANCE','HOME_INSURANCE','AUTO_INSURANCE',"
                                                        +
                                                        "      'LIFE_INSURANCE','TERM_INSURANCE','UTILITY','SUBSCRIPTION','SIP','RENT','CUSTOM'"
                                                        +
                                                        "    ); " +
                                                        "  END IF; " +
                                                        "END $$");

                        jdbcTemplate.execute(
                                        "DO $$ BEGIN " +
                                                        "  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_frequency') THEN "
                                                        +
                                                        "    CREATE TYPE payment_frequency AS ENUM (" +
                                                        "      'DAILY','WEEKLY','BIWEEKLY','MONTHLY','QUARTERLY','ANNUALLY'"
                                                        +
                                                        "    ); " +
                                                        "  END IF; " +
                                                        "END $$");

                        jdbcTemplate.execute(
                                        "DO $$ BEGIN " +
                                                        "  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'execution_status') THEN "
                                                        +
                                                        "    CREATE TYPE execution_status AS ENUM (" +
                                                        "      'PENDING','SUCCESS','FAILED','SKIPPED','CANCELLED'" +
                                                        "    ); " +
                                                        "  END IF; " +
                                                        "END $$");

                        jdbcTemplate.execute(
                                        "DO $$ BEGIN " +
                                                        "  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'reminder_type') THEN "
                                                        +
                                                        "    CREATE TYPE reminder_type AS ENUM ('EMAIL','IN_APP','PUSH'); "
                                                        +
                                                        "  END IF; " +
                                                        "END $$");

                        log.info("✔ AutoPay enum types ensured.");

                        // ── autopay_schedules ──────────────────────────────────────────────
                        jdbcTemplate.execute(
                                        "CREATE TABLE IF NOT EXISTS autopay_schedules (" +
                                                        "  id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                                                        "  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,"
                                                        +
                                                        "  payment_name VARCHAR(255) NOT NULL," +
                                                        "  payment_category payment_category NOT NULL," +
                                                        "  payment_provider VARCHAR(255)," +
                                                        "  account_number_encrypted TEXT," +
                                                        "  routing_number_encrypted TEXT," +
                                                        "  notes_encrypted TEXT," +
                                                        "  amount DECIMAL(15,2) NOT NULL CHECK (amount > 0)," +
                                                        "  currency VARCHAR(3) NOT NULL DEFAULT 'USD'," +
                                                        "  frequency payment_frequency NOT NULL," +
                                                        "  next_due_date DATE NOT NULL," +
                                                        "  due_day_of_month INTEGER CHECK (due_day_of_month BETWEEN 1 AND 31),"
                                                        +
                                                        "  auto_execute BOOLEAN NOT NULL DEFAULT FALSE," +
                                                        "  reminder_days_before INTEGER NOT NULL DEFAULT 3 CHECK (reminder_days_before >= 0),"
                                                        +
                                                        "  is_active BOOLEAN NOT NULL DEFAULT TRUE," +
                                                        "  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()," +
                                                        "  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()," +
                                                        "  CONSTRAINT uq_user_payment_provider UNIQUE (user_id, payment_name, payment_provider)"
                                                        +
                                                        ")");
                        jdbcTemplate.execute(
                                        "CREATE INDEX IF NOT EXISTS idx_autopay_schedules_user_id ON autopay_schedules(user_id)");
                        jdbcTemplate.execute(
                                        "CREATE INDEX IF NOT EXISTS idx_autopay_schedules_next_due_date ON autopay_schedules(next_due_date)");
                        jdbcTemplate.execute(
                                        "CREATE INDEX IF NOT EXISTS idx_autopay_schedules_active ON autopay_schedules(user_id, is_active)");
                        log.info("✔ autopay_schedules table ensured.");

                        // ── autopay_execution_logs ─────────────────────────────────────────
                        jdbcTemplate.execute(
                                        "CREATE TABLE IF NOT EXISTS autopay_execution_logs (" +
                                                        "  id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                                                        "  schedule_id UUID NOT NULL REFERENCES autopay_schedules(id) ON DELETE CASCADE,"
                                                        +
                                                        "  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,"
                                                        +
                                                        "  execution_date TIMESTAMPTZ NOT NULL DEFAULT NOW()," +
                                                        "  amount_paid DECIMAL(15,2) NOT NULL CHECK (amount_paid >= 0),"
                                                        +
                                                        "  status execution_status NOT NULL DEFAULT 'PENDING'," +
                                                        "  failure_reason VARCHAR(500)," +
                                                        "  plaid_transaction_id VARCHAR(255)," +
                                                        "  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()" +
                                                        ")");
                        jdbcTemplate.execute(
                                        "CREATE INDEX IF NOT EXISTS idx_exec_logs_schedule_id ON autopay_execution_logs(schedule_id)");
                        jdbcTemplate.execute(
                                        "CREATE INDEX IF NOT EXISTS idx_exec_logs_user_id ON autopay_execution_logs(user_id)");
                        // New columns added in Plaid Intelligence Bridge phase
                        jdbcTemplate.execute(
                                        "ALTER TABLE autopay_execution_logs " +
                                                        "ADD COLUMN IF NOT EXISTS stripe_payment_intent_id VARCHAR(255)");
                        jdbcTemplate.execute(
                                        "DO $$ BEGIN " +
                                                        "  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'plaid_verification_status') THEN "
                                                        +
                                                        "    CREATE TYPE plaid_verification_status AS ENUM ('UNVERIFIED','VERIFIED','NEEDS_REVIEW'); "
                                                        +
                                                        "  END IF; " +
                                                        "END $$");
                        jdbcTemplate.execute(
                                        "ALTER TABLE autopay_execution_logs " +
                                                        "ADD COLUMN IF NOT EXISTS plaid_verification_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED'");
                        jdbcTemplate.execute(
                                        "ALTER TABLE autopay_execution_logs " +
                                                        "ADD COLUMN IF NOT EXISTS plaid_matched_transaction_id VARCHAR(255)");
                        jdbcTemplate.execute(
                                        "CREATE INDEX IF NOT EXISTS idx_exec_logs_verification " +
                                                        "ON autopay_execution_logs(plaid_verification_status) " +
                                                        "WHERE plaid_verification_status = 'UNVERIFIED'");
                        log.info("✔ autopay_execution_logs table ensured.");

                        // ── autopay_reminders ──────────────────────────────────────────────
                        jdbcTemplate.execute(
                                        "CREATE TABLE IF NOT EXISTS autopay_reminders (" +
                                                        "  id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                                                        "  schedule_id UUID NOT NULL REFERENCES autopay_schedules(id) ON DELETE CASCADE,"
                                                        +
                                                        "  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,"
                                                        +
                                                        "  reminder_date DATE NOT NULL," +
                                                        "  reminder_type reminder_type NOT NULL DEFAULT 'IN_APP'," +
                                                        "  is_sent BOOLEAN NOT NULL DEFAULT FALSE," +
                                                        "  sent_at TIMESTAMPTZ," +
                                                        "  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()" +
                                                        ")");
                        jdbcTemplate.execute(
                                        "CREATE INDEX IF NOT EXISTS idx_reminders_user_id ON autopay_reminders(user_id)");
                        jdbcTemplate.execute(
                                        "CREATE INDEX IF NOT EXISTS idx_reminders_reminder_date ON autopay_reminders(reminder_date, is_sent)");
                        log.info("✔ autopay_reminders table ensured.");

                } catch (Exception e) {
                        log.error("Migration failed (non-fatal): {}", e.getMessage());
                }
                log.info("Database migration check complete.");
        }
}
