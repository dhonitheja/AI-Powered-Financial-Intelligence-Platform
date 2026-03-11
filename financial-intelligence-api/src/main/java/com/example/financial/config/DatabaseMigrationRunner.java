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
                // Retry up to 3 times with a 5-second delay to handle Supabase cold starts
                int maxRetries = 3;
                for (int attempt = 1; attempt <= maxRetries; attempt++) {
                        try {
                                runMigrations();
                                return; // success
                        } catch (Exception e) {
                                log.warn("Migration attempt {}/{} failed: {}", attempt, maxRetries, e.getMessage());
                                if (attempt < maxRetries) {
                                        try {
                                                Thread.sleep(5000);
                                        } catch (InterruptedException ie) {
                                                Thread.currentThread().interrupt();
                                                break;
                                        }
                                } else {
                                        log.error("All migration attempts failed (non-fatal): {}", e.getMessage());
                                }
                        }
                }
                log.info("Database migration check complete.");
        }

        private void runMigrations() {
                try {
                        jdbcTemplate.execute(
                                        "ALTER TABLE users ADD COLUMN IF NOT EXISTS two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE");
                        log.info("✔ Column 'two_factor_enabled' ensured.");

                        jdbcTemplate.execute(
                                        "ALTER TABLE users ADD COLUMN IF NOT EXISTS two_factor_secret VARCHAR(255)");
                        log.info("✔ Column 'two_factor_secret' ensured.");

                        // ── Stripe integration columns ─────────────────────────────────────
                        jdbcTemplate.execute(
                                        "ALTER TABLE users ADD COLUMN IF NOT EXISTS stripe_customer_id VARCHAR(255)");
                        jdbcTemplate.execute(
                                        "ALTER TABLE users ADD COLUMN IF NOT EXISTS stripe_default_payment_method VARCHAR(255)");
                        jdbcTemplate.execute(
                                        "CREATE INDEX IF NOT EXISTS idx_users_stripe_customer ON users(stripe_customer_id)");
                        log.info("✔ Stripe columns ensured.");

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

                        // ── notifications ──────────────────────────────────────────────────
                        jdbcTemplate.execute(
                                        "CREATE TABLE IF NOT EXISTS notifications (" +
                                                        "  id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                                                        "  user_id UUID NOT NULL REFERENCES users(id)," +
                                                        "  type VARCHAR(50) NOT NULL," +
                                                        "  title VARCHAR(255) NOT NULL," +
                                                        "  message TEXT NOT NULL," +
                                                        "  is_read BOOLEAN DEFAULT false," +
                                                        "  action_url VARCHAR(500)," +
                                                        "  created_at TIMESTAMPTZ NOT NULL DEFAULT now()," +
                                                        "  read_at TIMESTAMPTZ" +
                                                        ")");
                        jdbcTemplate.execute(
                                        "CREATE INDEX IF NOT EXISTS idx_notifications_user_unread " +
                                                        "ON notifications(user_id, is_read, created_at DESC) " +
                                                        "WHERE is_read = false");
                        jdbcTemplate.execute(
                                        "CREATE INDEX IF NOT EXISTS idx_notifications_user_created " +
                                                        "ON notifications(user_id, created_at DESC)");
                        log.info("✔ notifications table ensured.");

                        // ── users (onboarding) ─────────────────────────────────────────────
                        jdbcTemplate.execute(
                                        "ALTER TABLE users ADD COLUMN IF NOT EXISTS onboarding_completed BOOLEAN DEFAULT false");
                        log.info("✔ Onboarding columns ensured.");

                        // ── password reset tokens ────────────────────────────────────────
                        jdbcTemplate.execute(
                                        "CREATE TABLE IF NOT EXISTS password_reset_tokens (" +
                                        "  id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                                        "  token VARCHAR(255) NOT NULL UNIQUE," +
                                        "  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE," +
                                        "  expiry_date TIMESTAMPTZ NOT NULL," +
                                        "  created_at TIMESTAMPTZ NOT NULL DEFAULT now()," +
                                        "  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()" +
                                        ")");
                        jdbcTemplate.execute(
                                        "CREATE INDEX IF NOT EXISTS idx_password_reset_token ON password_reset_tokens(token)");
                        log.info("✔ Password reset tokens table ensured.");

                        // ── AI tables ──────────────────────────────────────────────
                        jdbcTemplate.execute(
                                        "CREATE TABLE IF NOT EXISTS chat_history (" +
                                        "  id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                                        "  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE," +
                                        "  role VARCHAR(20) NOT NULL," +
                                        "  content TEXT NOT NULL," +
                                        "  session_id UUID NOT NULL," +
                                        "  created_at TIMESTAMPTZ NOT NULL DEFAULT now()" +
                                        ")");
                        jdbcTemplate.execute(
                                        "CREATE INDEX IF NOT EXISTS idx_chat_history_user_session ON chat_history(user_id, session_id, created_at)");
                        log.info("✔ Chat history table ensured.");

                        jdbcTemplate.execute(
                                        "CREATE TABLE IF NOT EXISTS savings_goals (" +
                                        "  id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                                        "  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE," +
                                        "  goal_name VARCHAR(255) NOT NULL," +
                                        "  target_amount DECIMAL(15,2) NOT NULL CHECK (target_amount > 0)," +
                                        "  current_amount DECIMAL(15,2) NOT NULL DEFAULT 0," +
                                        "  currency VARCHAR(3) DEFAULT 'USD'," +
                                        "  target_date DATE," +
                                        "  is_active BOOLEAN DEFAULT true," +
                                        "  created_at TIMESTAMPTZ NOT NULL DEFAULT now()," +
                                        "  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()" +
                                        ")");
                        jdbcTemplate.execute(
                                        "CREATE INDEX IF NOT EXISTS idx_savings_goals_user ON savings_goals(user_id, is_active)");
                        log.info("✔ Savings goals table ensured.");

                        jdbcTemplate.execute(
                                        "CREATE TABLE IF NOT EXISTS spending_anomalies (" +
                                        "  id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                                        "  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE," +
                                        "  schedule_id UUID REFERENCES autopay_schedules(id) ON DELETE CASCADE," +
                                        "  anomaly_type VARCHAR(100) NOT NULL," +
                                        "  description VARCHAR(500) NOT NULL," +
                                        "  detected_at TIMESTAMPTZ NOT NULL DEFAULT now()," +
                                        "  is_acknowledged BOOLEAN DEFAULT false," +
                                        "  acknowledged_at TIMESTAMPTZ" +
                                        ")");
                        jdbcTemplate.execute(
                                        "CREATE INDEX IF NOT EXISTS idx_anomalies_user_unread ON spending_anomalies(user_id, is_acknowledged) WHERE is_acknowledged = false");
                        log.info("✔ Spending anomalies table ensured.");

                } catch (Exception e) {
                        log.error("Migration failed (non-fatal): {}", e.getMessage());
                        throw new RuntimeException("Migration failed: " + e.getMessage(), e);
                }
        }
}
