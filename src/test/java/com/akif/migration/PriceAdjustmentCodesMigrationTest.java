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

@DisplayName("Price adjustment code migration")
class PriceAdjustmentCodesMigrationTest {

    private static final String INSERT_RENTAL = "SET session_replication_role = replica; "
            + "INSERT INTO car_rental.rentals (create_time, update_time, is_deleted, version, user_id, car_id, "
            + "car_brand, car_model, car_license_plate, user_email, user_full_name, start_date, end_date, days, "
            + "currency, daily_price, total_price, status, late_return_status, price_adjustments) "
            + "VALUES (NOW(), NOW(), FALSE, 0, 1, 1, 'B', 'M', '34ZZ%d', 'a@b.c', 'N', '2031-01-01', '2031-01-03', "
            + "3, 'TRY', 10, 30, 'RETURNED', 'ON_TIME', %s)";

    @Test
    @DisplayName("stored display names become stable codes and other entries are untouched")
    void mapsDisplayNamesToCodes() throws SQLException {
        String url = createDatabase("price_adjustment_codes");
        Flyway.configure().dataSource(url, postgres().getUsername(), postgres().getPassword())
                .schemas("car_rental").locations("classpath:db/migration").target("26").load().migrate();
        String legacy = "'[{\"code\":\"Season Pricing\",\"label\":\"Peak\",\"multiplier\":1.25,\"amount\":5.00},"
                + "{\"code\":\"Early Booking\",\"label\":\"x\",\"multiplier\":0.85,\"amount\":-1},"
                + "{\"code\":\"Duration Discount\",\"label\":\"x\",\"multiplier\":0.9,\"amount\":-1},"
                + "{\"code\":\"Weekend Pricing\",\"label\":\"x\",\"multiplier\":1.15,\"amount\":1},"
                + "{\"code\":\"Demand Pricing\",\"label\":\"x\",\"multiplier\":1.2,\"amount\":1},"
                + "{\"code\":\"PRICE_LIMIT\",\"label\":\"Daily price limit applied\",\"multiplier\":null,\"amount\":-2}]'";
        execute(url, String.format(INSERT_RENTAL, 1, legacy));
        execute(url, String.format(INSERT_RENTAL, 2, "NULL"));

        Flyway.configure().dataSource(url, postgres().getUsername(), postgres().getPassword())
                .schemas("car_rental").locations("classpath:db/migration").load().migrate();

        String migrated = text(url, "SELECT price_adjustments FROM car_rental.rentals WHERE car_license_plate = '34ZZ1'");
        assertThat(migrated).contains("\"code\":\"SEASON\"", "\"code\":\"EARLY_BOOKING\"",
                "\"code\":\"DURATION_DISCOUNT\"", "\"code\":\"WEEKEND\"", "\"code\":\"DEMAND\"",
                "\"code\":\"PRICE_LIMIT\"", "\"label\":\"Peak\"");
        assertThat(migrated).doesNotContain("Season Pricing", "Early Booking", "Duration Discount",
                "Weekend Pricing", "Demand Pricing");
        assertThat(text(url, "SELECT COALESCE(price_adjustments, 'null') FROM car_rental.rentals "
                + "WHERE car_license_plate = '34ZZ2'")).isEqualTo("null");
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

    private static String text(String url, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, postgres().getUsername(),
                postgres().getPassword()); Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }
}
