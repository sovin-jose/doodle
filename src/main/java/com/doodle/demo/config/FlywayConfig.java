package com.doodle.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    /**
     * Auto-migrate only for SQLite. For any other JDBC vendor, callers must trigger
     * migrations manually via POST /admin/migrate — this keeps schema changes explicit
     * and reviewable in shared environments.
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(Environment env) {
        return flyway -> {
            String url = env.getProperty("spring.datasource.url", "");
            boolean isSqlite = url.startsWith("jdbc:sqlite");
            boolean forceAuto = Boolean.parseBoolean(env.getProperty("app.flyway.auto-migrate", "false"));

            if (isSqlite || forceAuto) {
                log.info("Flyway auto-migrate running (sqlite={}, forced={})", isSqlite, forceAuto);
                flyway.migrate();
            } else {
                log.info("Flyway auto-migrate skipped for non-SQLite datasource ({}). " +
                        "Trigger POST /admin/migrate to apply migrations.", url);
            }
        };
    }
}
