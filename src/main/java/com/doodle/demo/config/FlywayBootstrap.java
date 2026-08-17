package com.doodle.demo.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Boot 4.1's Flyway auto-configuration isn't running for this app (no Flyway
 * logs at startup, migration tables never created), so we wire Flyway
 * explicitly. `initMethod = "migrate"` guarantees the migration runs before
 * the JPA EntityManagerFactory starts querying the schema.
 */
@Configuration
public class FlywayBootstrap {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }
}
