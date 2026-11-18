package com.akif.migration;

import com.akif.testsupport.TestcontainersConfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Flyway seed policy")
class FlywaySeedPolicyTest {

    private static final String SEED_LOCATION = "classpath:db/seed";
    private static final List<String> SEED_FILES = List.of("R__seed_01_users.sql", "R__seed_02_cars.sql",
            "R__seed_03_rentals.sql", "R__seed_04_payments.sql", "R__seed_05_damages.sql",
            "R__seed_06_admin_data.sql");

    @Test
    @DisplayName("dev profile adds the seed location and no other profile does")
    void seedLocationIsConfiguredOnlyForDevProfile() throws IOException {
        assertThat(load("application-dev.properties").getProperty("spring.flyway.locations", ""))
                .contains(SEED_LOCATION);
        assertThat(load("application.properties").getProperty("spring.flyway.locations", ""))
                .doesNotContain("seed");
        assertThat(load("application-prod.properties").getProperty("spring.flyway.locations", ""))
                .doesNotContain("seed");
        assertThat(load("application-test.properties").getProperty("spring.flyway.locations", ""))
                .doesNotContain("seed");
    }

    @Test
    @DisplayName("seed image URLs use https and avoid the retired focus.bg host")
    void seedImageUrlsAreStable() throws IOException {
        for (String name : List.of("R__seed_02_cars.sql", "R__seed_03_rentals.sql")) {
            String sql = new String(new ClassPathResource("db/seed/" + name).getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            assertThat(sql).doesNotContain("focus.bg").doesNotContain("http://");
        }
    }

    @Test
    @DisplayName("seed data uses only reserved example domains and fictitious phone numbers")
    void seedContainsNoRealLookingContactData() throws IOException {
        Pattern email = Pattern.compile("[A-Za-z0-9._+-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
        Pattern phone = Pattern.compile("'(\\+\\d{7,15})'");
        List<String> reserved = List.of("example.com", "example.org", "example.net");
        List<String> offenders = new ArrayList<>();
        for (String name : List.of("R__seed_01_users.sql", "R__seed_02_cars.sql", "R__seed_03_rentals.sql",
                "R__seed_04_payments.sql", "R__seed_05_damages.sql", "R__seed_06_admin_data.sql")) {
            String sql = new String(new ClassPathResource("db/seed/" + name).getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            Matcher emails = email.matcher(sql);
            while (emails.find()) {
                if (!reserved.contains(emails.group(1).toLowerCase())) {
                    offenders.add(name + ": " + emails.group());
                }
            }
            Matcher phones = phone.matcher(sql);
            while (phones.find()) {
                if (!phones.group(1).startsWith("+1555010")) {
                    offenders.add(name + ": " + phones.group(1));
                }
            }
        }
        assertThat(offenders).as("seed contact data must be fictitious").isEmpty();
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

    @Test
    @DisplayName("editing a seed file re-runs it without duplicating rows and replaces legacy rows")
    void editedSeedsRerunWithoutDuplicates(@TempDir Path seedDirectory) throws Exception {
        for (String name : SEED_FILES) {
            Files.copy(new ClassPathResource("db/seed/" + name).getInputStream(), seedDirectory.resolve(name));
        }
        String url = createDatabase("edited_seed");
        Flyway flyway = Flyway.configure()
                .dataSource(url, user(), password())
                .schemas("car_rental")
                .locations("classpath:db/migration", "filesystem:" + seedDirectory)
                .load();
        flyway.migrate();
        Map<String, Long> before = seedRowCounts(url);
        execute(url, "INSERT INTO car_rental.rentals (create_time, update_time, is_deleted, version, user_id, car_id, "
                + "car_brand, car_model, car_license_plate, user_email, user_full_name, start_date, end_date, days, "
                + "currency, daily_price, total_price, status, late_return_status) "
                + "SELECT NOW(), NOW(), FALSE, 0, u.id, c.id, 'Legacy', 'Row', c.license_plate, u.email, 'Legacy', "
                + "'2024-01-01', '2024-01-03', 3, 'USD', 10, 30, 'RETURNED', 'ON_TIME' "
                + "FROM car_rental.users u, car_rental.car c WHERE u.email = 'ahmet.yilmaz@example.com' "
                + "AND c.license_plate = '34ABC123'");

        for (String name : SEED_FILES) {
            Files.writeString(seedDirectory.resolve(name),
                    Files.readString(seedDirectory.resolve(name)) + "\nSELECT 1;\n");
        }

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(SEED_FILES.size());
        assertThat(seedRowCounts(url)).isEqualTo(before);
        assertThat(count(url, "SELECT COUNT(*) FROM car_rental.rentals WHERE car_brand = 'Legacy'")).isZero();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    private static Map<String, Long> seedRowCounts(String url) throws SQLException {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String table : List.of("users", "car", "rentals", "payments", "damage_reports", "admin_notes",
                "penalty_waivers")) {
            counts.put(table, count(url, "SELECT COUNT(*) FROM car_rental." + table));
        }
        return counts;
    }

    private static void execute(String url, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, user(), password());
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
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
