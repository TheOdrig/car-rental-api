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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Case-insensitive user email migration")
class UsersEmailMigrationTest {

    private static final String INSERT_USER = "INSERT INTO car_rental.users (username, email, create_time) "
            + "VALUES ('%s', '%s', NOW() + INTERVAL '%d seconds')";

    @Test
    @DisplayName("existing duplicates keep the oldest account and the rest are renamed, not deleted")
    void renamesLaterDuplicates() throws SQLException {
        String url = createDatabase("users_email_duplicates");
        Flyway.configure().dataSource(url, postgres().getUsername(), postgres().getPassword())
                .schemas("car_rental").locations("classpath:db/migration").target("13").load().migrate();
        execute(url, String.format(INSERT_USER, "first", "Victim@example.com", 0));
        execute(url, String.format(INSERT_USER, "second", "victim@example.com", 5));
        execute(url, String.format(INSERT_USER, "third", "other@example.com", 10));

        Flyway.configure().dataSource(url, postgres().getUsername(), postgres().getPassword())
                .schemas("car_rental").locations("classpath:db/migration").load().migrate();

        assertThat(count(url, "SELECT COUNT(*) FROM car_rental.users")).isEqualTo(3);
        assertThat(count(url, "SELECT COUNT(*) FROM car_rental.users WHERE username = 'first' "
                + "AND email = 'Victim@example.com'")).isEqualTo(1);
        assertThat(count(url, "SELECT COUNT(*) FROM car_rental.users WHERE username = 'second' "
                + "AND email LIKE 'dup%victim@example.com'")).isEqualTo(1);
        assertThat(count(url, "SELECT COUNT(*) FROM car_rental.users WHERE username = 'third' "
                + "AND email = 'other@example.com'")).isEqualTo(1);
        assertThatThrownBy(() -> execute(url, String.format(INSERT_USER, "fourth", "OTHER@example.com", 20)))
                .isInstanceOf(SQLException.class);
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
