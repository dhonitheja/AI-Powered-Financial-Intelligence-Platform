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
 * transaction mode). This ensures the 2FA columns always exist.
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

                } catch (Exception e) {
                        log.error("Migration failed (non-fatal): {}", e.getMessage());
                }
                log.info("Database migration check complete.");
        }
}
