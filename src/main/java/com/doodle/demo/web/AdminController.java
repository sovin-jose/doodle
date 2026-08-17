package com.doodle.demo.web;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final Flyway flyway;

    public AdminController(Flyway flyway) {
        this.flyway = flyway;
    }

    @PostMapping("/migrate")
    public Map<String, Object> migrate() {
        MigrateResult result = flyway.migrate();
        return Map.of(
                "initialSchemaVersion", String.valueOf(result.initialSchemaVersion),
                "targetSchemaVersion", String.valueOf(result.targetSchemaVersion),
                "migrationsExecuted", result.migrationsExecuted,
                "success", result.success,
                "migrations", result.migrations.stream()
                        .map(m -> (Object) Map.of(
                                "version", String.valueOf(m.version),
                                "description", m.description,
                                "type", m.type,
                                "category", m.category))
                        .toList()
        );
    }

    @GetMapping("/migrations")
    public List<Map<String, Object>> migrations() {
        return java.util.Arrays.stream(flyway.info().all())
                .map(info -> (Map<String, Object>) Map.of(
                        "version", String.valueOf(info.getVersion()),
                        "description", info.getDescription(),
                        "state", info.getState().getDisplayName(),
                        "installedOn", String.valueOf(info.getInstalledOn())))
                .toList();
    }
}
