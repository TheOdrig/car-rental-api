package com.akif.shared.config;

import com.akif.CarRentalApplication;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Production configuration is checked before any migration runs")
class ProdConfigurationBeforeMigrationTest {

    @Test
    @DisplayName("a live Stripe key stops the prod boot and leaves the database untouched")
    void invalidConfigurationRunsNoMigration() throws SQLException {
        PostgreSQLContainer<?> postgres = TestcontainersConfiguration.postgres();
        String database = "prod_validation_first";
        try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword()); Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + database);
            statement.execute("CREATE DATABASE " + database);
        }
        String url = postgres.getJdbcUrl().replace("/" + postgres.getDatabaseName(), "/" + database);

        assertThatThrownBy(() -> new SpringApplicationBuilder(CarRentalApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("prod")
                .run(
                        "--spring.datasource.url=" + url,
                        "--spring.datasource.username=" + postgres.getUsername(),
                        "--spring.datasource.password=" + postgres.getPassword(),
                        "--jwt.secret=k7Hq2xVn9Zp4Lm8Rt3Wd6Yb1Cs5Fg0Je",
                        "--oauth2.state.secret=Qw3rTy7uIo9pAs2dFg5hJk8lZx1cVb4n",
                        "--cors.allowed-origins=https://rent.example.com",
                        "--stripe.api.key=sk_live_51Habcdefghijklmnop",
                        "--stripe.webhook.secret=whsec_abcdefghijklmnop",
                        "--stripe.metadata.environment=prod",
                        "--r2.endpoint=https://acc.r2.cloudflarestorage.com",
                        "--r2.bucket-name=bucket",
                        "--r2.access-key-id=id",
                        "--r2.secret-access-key=key",
                        "--email.from=noreply@example.com",
                        "--spring.mail.password=mail-key",
                        "--exchange-rate.api.key=rate-key",
                        "--oauth2.google.client-id=g-id",
                        "--oauth2.google.client-secret=g-secret",
                        "--oauth2.github.client-id=gh-id",
                        "--oauth2.github.client-secret=gh-secret",
                        "--security.rate-limit.trust-forwarded-headers=false",
                        "--security.rate-limit.trusted-proxy-hops=1",
                        "--APP_BASE_URL=https://rent.example.com"))
                .hasStackTraceContaining("stripe.api.key must be a Stripe test-mode key");

        assertThat(count(url, "SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_name = 'flyway_schema_history'")).isZero();
        assertThat(count(url, "SELECT COUNT(*) FROM information_schema.schemata "
                + "WHERE schema_name = 'car_rental'")).isZero();
    }

    private static long count(String url, String sql) throws SQLException {
        PostgreSQLContainer<?> postgres = TestcontainersConfiguration.postgres();
        try (Connection connection = DriverManager.getConnection(url, postgres.getUsername(), postgres.getPassword());
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
