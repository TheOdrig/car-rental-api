package com.akif.migration;

import com.akif.testsupport.TestcontainersConfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Flyway seed policy")
class FlywaySeedPolicyTest {

    private static final String SEED_LOCATION = "classpath:db/seed";

    @Test
    @DisplayName("dev profile adds the seed location and no other profile does")
    void seedLocationIsConfiguredOnlyForDevProfile() throws IOException {
        assertThat(load("application-dev.properties").getProperty("spring.flyway.locations"))
                .contains(SEED_LOCATION);
        assertThat(load("application.properties").getProperty("spring.flyway.locations"))
                .doesNotContain("seed");
        assertThat(load("application-production.properties").getProperty("spring.flyway.locations"))
                .doesNotContain("seed");
        assertThat(load("application-test.properties").getProperty("spring.flyway.locations"))
                .doesNotContain("seed");
    }

    @Test
    @DisplayName("versioned migrations alone create the schema without any user")
    void baselineCreatesNoUsers() throws SQLException {
        String url = createDatabase("baseline_only");

        Flyway.configure()
                .dataSource(url, user(), password())
                .schemas("car_rental")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        assertThat(count(url, "SELECT COUNT(*) FROM car_rental.users")).isZero();
        assertThat(count(url, "SELECT COUNT(*) FROM car_rental.car")).isZero();
    }

    @Test
    @DisplayName("seed migrations apply on top of the baseline and stay idempotent")
    void seedAppliesOnBaseline() throws SQLException {
        String url = createDatabase("with_seed");
        Flyway flyway = Flyway.configure()
                .dataSource(url, user(), password())
                .schemas("car_rental")
                .locations("classpath:db/migration", SEED_LOCATION)
                .load();

        flyway.migrate();

        assertThat(count(url, "SELECT COUNT(*) FROM car_rental.users u JOIN car_rental.user_roles r ON r.user_id = u.id "
                + "WHERE u.username = 'admin' AND r.role = 'ADMIN'")).isEqualTo(1);
        assertThat(count(url, "SELECT COUNT(*) FROM car_rental.car")).isPositive();
        assertThat(count(url, "SELECT COUNT(*) FROM car_rental.rentals")).isPositive();
        assertThat(count(url, "SELECT COUNT(*) FROM car_rental.payments")).isPositive();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    private static Properties load(String name) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = new ClassPathResource(name).getInputStream()) {
            properties.load(in);
        }
        return properties;
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

    private static long count(String url, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, user(), password());
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
