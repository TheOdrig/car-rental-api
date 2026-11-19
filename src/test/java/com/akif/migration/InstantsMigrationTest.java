package com.akif.migration;

import com.akif.testsupport.TestcontainersConfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Instant columns migration")
class InstantsMigrationTest {

    private static final String INSERT_PAYMENT = "SET session_replication_role = replica; "
            + "INSERT INTO car_rental.payments (rental_id, kind, amount, currency, status, create_time, update_time, "
            + "is_deleted, version) VALUES (1, 'RENTAL', 100, 'TRY', 'PENDING', '2031-03-01 08:00:00', "
            + "'2031-03-05 09:30:00', FALSE, 0)";

    @Test
    @DisplayName("stored zone-less values are read as UTC and no zone-less timestamp column remains")
    void convertsZoneLessTimestampsToUtcInstants() throws SQLException {
        String url = createDatabase("instants_with_time_zone");
        Flyway.configure().dataSource(url, postgres().getUsername(), postgres().getPassword())
                .schemas("car_rental").locations("classpath:db/migration").target("18").load().migrate();
        execute(url, INSERT_PAYMENT);

        Flyway.configure().dataSource(url, postgres().getUsername(), postgres().getPassword())
                .schemas("car_rental").locations("classpath:db/migration").target("latest").load().migrate();

        assertThat(count(url, "SET TIME ZONE 'Europe/Istanbul'",
                "SELECT COUNT(*) FROM car_rental.payments WHERE create_time = TIMESTAMPTZ '2031-03-01 08:00:00+00' "
                        + "AND update_time = TIMESTAMPTZ '2031-03-05 09:30:00+00'")).isEqualTo(1);
        assertThat(count(url, "SELECT 1", "SELECT COUNT(*) FROM information_schema.columns c "
                + "JOIN information_schema.tables t ON t.table_schema = c.table_schema AND t.table_name = c.table_name "
                + "WHERE c.table_schema = 'car_rental' AND t.table_type = 'BASE TABLE' "
                + "AND c.table_name <> 'flyway_schema_history' "
                + "AND c.data_type = 'timestamp without time zone'")).isZero();
        assertThat(count(url, "SELECT 1", "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = 'car_rental' AND table_name = 'payments' AND column_name = 'create_time' "
                + "AND data_type = 'timestamp with time zone'")).isEqualTo(1);
    }

    private static PostgreSQLContainer<?> postgres() {
        return TestcontainersConfiguration.postgres();
    }

    private static String createDatabase(String name) throws SQLException {
        try (Connection connection = DriverManager.getConnection(postgres().getJdbcUrl(), postgres().getUsername(),
                postgres().getPassword()); Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + name);
            statement.execute("CREATE DATABASE " + name);
        }
        return postgres().getJdbcUrl().replace("/" + postgres().getDatabaseName(), "/" + name);
    }

    private static void execute(String url, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, postgres().getUsername(),
                postgres().getPassword()); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static long count(String url, String setup, String query) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, postgres().getUsername(),
                postgres().getPassword()); Statement statement = connection.createStatement()) {
            statement.execute(setup);
            try (ResultSet resultSet = statement.executeQuery(query)) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }
}
