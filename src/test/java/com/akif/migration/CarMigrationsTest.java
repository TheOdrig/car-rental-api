package com.akif.migration;

import com.akif.testsupport.TestcontainersConfiguration;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Car and index migrations")
class CarMigrationsTest {

    private static final String INSERT_CAR = "INSERT INTO car_rental.car (create_time, update_time, is_deleted, "
            + "version, license_plate, brand, model, production_year, price, currency_type, damage_price, "
            + "car_status_type) VALUES (NOW(), NOW(), FALSE, 0, '%s', 'Brand', 'Model', 2020, 50, 'USD', %s, '%s')";

    @Test
    @DisplayName("RENTED and RESERVED cars become AVAILABLE and the status check is enforced")
    void convertsGalleryStatuses() throws SQLException {
        String url = createDatabase("fleet_status_convert");
        flyway(url).target("10").load().migrate();
        execute(url, String.format(INSERT_CAR, "34AAA111", "0", "RENTED"));
        execute(url, String.format(INSERT_CAR, "34AAA112", "0", "RESERVED"));
        execute(url, String.format(INSERT_CAR, "34AAA113", "0", "MAINTENANCE"));

        flyway(url).load().migrate();

        assertThat(count(url, "SELECT COUNT(*) FROM car_rental.car WHERE car_status_type = 'AVAILABLE'")).isEqualTo(2);
        assertThat(count(url, "SELECT COUNT(*) FROM car_rental.car WHERE car_status_type = 'MAINTENANCE'")).isEqualTo(1);
        assertThat(count(url, "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = 'car_rental' AND table_name = 'car' AND column_name = 'damage_price'")).isZero();
        assertThatThrownBy(() -> execute(url, "INSERT INTO car_rental.car (create_time, update_time, is_deleted, "
                + "version, license_plate, brand, model, production_year, price, currency_type, car_status_type) "
                + "VALUES (NOW(), NOW(), FALSE, 0, '34AAA114', 'B', 'M', 2020, 50, 'USD', 'RENTED')"))
                .isInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("a non-zero damage price blocks the migration instead of dropping data")
    void rejectsNonZeroDamagePrice() throws SQLException {
        String url = createDatabase("fleet_status_reject");
        flyway(url).target("10").load().migrate();
        execute(url, String.format(INSERT_CAR, "34BBB111", "1500", "AVAILABLE"));

        assertThatThrownBy(() -> flyway(url).load().migrate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("damage_price");
    }

    @Test
    @DisplayName("the hot path indexes are created and partial on live rows")
    void createsHotPathIndexes() throws SQLException {
        String url = createDatabase("hot_path_indexes");

        flyway(url).load().migrate();

        assertThat(count(url, "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'car_rental' AND indexname IN "
                + "('idx_payments_create_time', 'idx_rentals_user_create_time', 'idx_rentals_blocking_dates')"))
                .isEqualTo(3);
    }

    private static FluentConfiguration flyway(String url) {
        return Flyway.configure()
                .dataSource(url, postgres().getUsername(), postgres().getPassword())
                .schemas("car_rental")
                .locations("classpath:db/migration");
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
