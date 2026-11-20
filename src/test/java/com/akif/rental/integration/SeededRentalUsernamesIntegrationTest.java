package com.akif.rental.integration;

import com.akif.CarRentalApplication;
import com.akif.shared.enums.Role;
import com.akif.shared.security.JwtTokenService;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class, properties = {
        "spring.flyway.locations=classpath:db/migration,classpath:db/seed"
})
@Import(SeededRentalUsernamesIntegrationTest.SeededDatabase.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Seeded rentals")
class SeededRentalUsernamesIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class SeededDatabase {

        @Bean
        @Primary
        JdbcConnectionDetails seededConnectionDetails() throws SQLException {
            PostgreSQLContainer<?> postgres = TestcontainersConfiguration.postgres();
            try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
                    postgres.getPassword());
                    Statement statement = connection.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS seeded_rentals");
                statement.execute("CREATE DATABASE seeded_rentals");
            }
            String url = postgres.getJdbcUrl().replace("/" + postgres.getDatabaseName(), "/seeded_rentals");
            return new JdbcConnectionDetails() {
                @Override
                public String getUsername() {
                    return postgres.getUsername();
                }

                @Override
                public String getPassword() {
                    return postgres.getPassword();
                }

                @Override
                public String getJdbcUrl() {
                    return url;
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTokenService tokenService;

    @Test
    @DisplayName("every seeded rental carries the customer username on the admin list")
    void everySeededRentalHasUsername() throws Exception {
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM car_rental.rentals", Long.class);
        assertThat(total).isPositive();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM car_rental.rentals WHERE user_username IS NULL", Long.class)).isZero();
        Long adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_rental.users WHERE username = 'admin'", Long.class);
        String token = tokenService.generateAccessToken(new UsernamePasswordAuthenticationToken("admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + Role.ADMIN.name()))), adminId);

        mockMvc.perform(get("/api/v1/rentals/admin").param("size", "100")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(total.intValue()))
                .andExpect(jsonPath("$.content[?(!@.userSummary.username)]").isEmpty());
    }

    @Test
    @DisplayName("seeded rentals count days inclusively and price them from the daily rate")
    void seededDaysAndPricesAreConsistent() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM car_rental.rentals WHERE id >= 900001 AND "
                        + "(days <> (end_date - start_date) + 1 OR total_price <> days * daily_price)",
                Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM car_rental.payments p JOIN car_rental.rentals r ON r.id = p.rental_id "
                        + "WHERE p.id >= 910001 AND p.status = 'CAPTURED' AND r.penalty_paid = FALSE "
                        + "AND p.amount <> r.total_price",
                Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM car_rental.payments WHERE id >= 910001 AND rental_id IS NULL",
                Long.class)).isZero();
    }
}
