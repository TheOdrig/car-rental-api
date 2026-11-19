package com.akif.testsupport;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;

public final class TestDatabase {

    private static final String SCHEMA = "car_rental";
    private static final List<String> KEPT_TABLES = List.of("flyway_schema_history", "installation_identity");
    private static final Duration MAX_WAIT = Duration.ofSeconds(10);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(250);

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
        await().atMost(MAX_WAIT).pollInterval(POLL_INTERVAL).until(() -> truncated(jdbcTemplate, targets));
    }

    private static boolean truncated(JdbcTemplate jdbcTemplate, String targets) {
        try {
            jdbcTemplate.execute("TRUNCATE TABLE " + targets + " RESTART IDENTITY CASCADE");
            return true;
        } catch (DataAccessException e) {
            return false;
        }
    }
}
