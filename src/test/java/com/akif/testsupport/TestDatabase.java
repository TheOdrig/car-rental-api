package com.akif.testsupport;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public final class TestDatabase {

    private static final String SCHEMA = "car_rental";
    private static final List<String> KEPT_TABLES = List.of("flyway_schema_history", "installation_identity");
    private static final int MAX_ATTEMPTS = 40;
    private static final long RETRY_PAUSE_MILLIS = 250;

    private TestDatabase() {
    }

    public static void truncateAll(JdbcTemplate jdbcTemplate) {
        String targets = jdbcTemplate.queryForList(
                        "SELECT table_name FROM information_schema.tables "
                                + "WHERE table_schema = ? AND table_type = 'BASE TABLE'", String.class, SCHEMA)
                .stream()
                .filter(table -> !KEPT_TABLES.contains(table))
                .map(table -> SCHEMA + "." + table)
                .reduce((left, right) -> left + ", " + right)
                .orElse(null);
        if (targets == null) {
            return;
        }
        DataAccessException last = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                jdbcTemplate.execute("TRUNCATE TABLE " + targets + " RESTART IDENTITY CASCADE");
                return;
            } catch (DataAccessException e) {
                last = e;
                pause();
            }
        }
        throw last;
    }

    private static void pause() {
        try {
            Thread.sleep(RETRY_PAUSE_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to truncate", e);
        }
    }
}
