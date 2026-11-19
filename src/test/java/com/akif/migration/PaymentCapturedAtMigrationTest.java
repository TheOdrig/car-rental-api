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

@DisplayName("Payment capture time migration")
class PaymentCapturedAtMigrationTest {

    private static final String INSERT_PAYMENT = "SET session_replication_role = replica; "
            + "INSERT INTO car_rental.payments (rental_id, kind, amount, currency, status, create_time, update_time, "
            + "is_deleted, version) VALUES (%d, 'RENTAL', 100, 'TRY', '%s', '2031-03-01 08:00:00', "
            + "'2031-03-05 09:30:00', FALSE, 0)";

    @Test
    @DisplayName("captured and refunded payments are backfilled from their last update and the others stay empty")
    void backfillsCapturedPayments() throws SQLException {
        String url = createDatabase("payment_captured_at");
        Flyway.configure().dataSource(url, postgres().getUsername(), postgres().getPassword())
                .schemas("car_rental").locations("classpath:db/migration").target("12").load().migrate();
        String[] statuses = {"CAPTURED", "REFUNDED", "PENDING", "AUTHORIZED", "CANCELLED"};
        for (int index = 0; index < statuses.length; index++) {
            execute(url, String.format(INSERT_PAYMENT, index + 1, statuses[index]));
        }

        flyway(url).migrate();

        assertThat(count(url, "SELECT COUNT(*) FROM car_rental.payments WHERE captured_at = update_time "
                + "AND status IN ('CAPTURED', 'REFUNDED')")).isEqualTo(2);
        assertThat(count(url, "SELECT COUNT(*) FROM car_rental.payments WHERE captured_at IS NULL "
                + "AND status IN ('PENDING', 'AUTHORIZED', 'CANCELLED')")).isEqualTo(3);
        assertThat(count(url, "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'car_rental' "
                + "AND indexname = 'idx_payments_captured_at'")).isEqualTo(1);
    }

    private static Flyway flyway(String url) {
        return Flyway.configure()
                .dataSource(url, postgres().getUsername(), postgres().getPassword())
                .schemas("car_rental")
                .locations("classpath:db/migration")
                .target("latest")
                .load();
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

    private static long count(String url, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, postgres().getUsername(),
                postgres().getPassword()); Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
