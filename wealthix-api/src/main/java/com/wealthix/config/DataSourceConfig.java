package com.wealthix.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

import org.springframework.context.annotation.Profile;

/**
 * Custom DataSource for Supabase Pooler connections.
 *
 * Uses PGSimpleDataSource wrapped in HikariCP so the full dotted username
 * (e.g. "postgres.PROJECT_REF") is preserved — Spring's default auto-config
 * passes username via DriverManager which truncates at the dot.
 */
@Configuration
@Profile("prod")
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        log.info("[DataSource] Connecting to {} as user '{}'", jdbcUrl, username);

        PGSimpleDataSource pgDs = new PGSimpleDataSource();
        pgDs.setURL(jdbcUrl);
        pgDs.setUser(username);
        pgDs.setPassword(password);
        pgDs.setSslMode("require");
        pgDs.setPrepareThreshold(0);

        HikariConfig config = new HikariConfig();
        config.setDataSource(pgDs);
        config.setPoolName("WealthixPool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(30000);
        config.setConnectionInitSql("SELECT 1");

        return new HikariDataSource(config);
    }
}
