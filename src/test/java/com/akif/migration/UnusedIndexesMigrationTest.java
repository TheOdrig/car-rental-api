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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unused index cleanup")
class UnusedIndexesMigrationTest {

    private static final List<String> DROPPED = List.of("idx_car_brand", "idx_car_status", "idx_car_price",
            "index_car_year", "index_car_create_time", "idx_car_transmission_type", "idx_car_body_type",
            "idx_car_fuel_type", "idx_car_seats", "idx_users_enabled", "idx_alert_severity", "idx_alert_acknowledged");

    private static final List<String> KEPT = List.of("uk_car_license_plate", "uk_car_vin", "uk_users_username",
            "uk_users_email_lower", "idx_rentals_blocking_dates", "idx_rentals_user_create_time",
            "idx_alert_active", "idx_alert_type", "idx_payments_rental");

    @Test
    @DisplayName("the dropped indexes are gone, the ones backing real queries and constraints remain")
    void droppedAndKeptIndexes() throws SQLException {
        String url = createDatabase("index_cleanup");
        Flyway.configure().dataSource(url, user(), password()).schemas("car_rental")
                .locations("classpath:db/migration").load().migrate();

        List<String> present = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(url, user(), password());
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT indexname FROM pg_indexes WHERE schemaname = 'car_rental'")) {
            while (rows.next()) {
                present.add(rows.getString(1));
            }
        }

        assertThat(present).doesNotContainAnyElementsOf(DROPPED);
        assertThat(present).containsAll(KEPT);
        assertThat(present).doesNotContain("uk_users_email");
    }

    @Test
    @DisplayName("case-insensitive email uniqueness is still enforced by the remaining index")
    void emailUniquenessSurvives() throws SQLException {
        String url = createDatabase("index_cleanup_email");
        Flyway.configure().dataSource(url, user(), password()).schemas("car_rental")
                .locations("classpath:db/migration").load().migrate();

        try (Connection connection = DriverManager.getConnection(url, user(), password());
                Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO car_rental.users (username, email, create_time) "
                    + "VALUES ('one', 'Same@example.com', NOW())");
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> statement.execute(
                            "INSERT INTO car_rental.users (username, email, create_time) "
                                    + "VALUES ('two', 'same@example.com', NOW())"))
                    .isInstanceOf(SQLException.class);
        }
    }

    private static PostgreSQLContainer<?> postgres() {
        return TestcontainersConfiguration.postgres();
    }

    private static String user() {
        return postgres().getUsername();
    }

    private static String password() {
        return postgres().getPassword();
    }

    private static String createDatabase(String name) throws SQLException {
        try (Connection connection = DriverManager.getConnection(postgres().getJdbcUrl(), user(), password());
                Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + name);
            statement.execute("CREATE DATABASE " + name);
        }
        return postgres().getJdbcUrl().replace("/" + postgres().getDatabaseName(), "/" + name);
    }
}
