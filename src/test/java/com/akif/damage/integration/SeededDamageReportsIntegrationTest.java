package com.akif.damage.integration;

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
@Import(SeededDamageReportsIntegrationTest.SeededDatabase.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Seeded damage reports")
class SeededDamageReportsIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class SeededDatabase {

        @Bean
        @Primary
        JdbcConnectionDetails seededConnectionDetails() throws SQLException {
            PostgreSQLContainer<?> postgres = TestcontainersConfiguration.postgres();
            try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
                    postgres.getPassword());
                    Statement statement = connection.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS seeded_damages");
                statement.execute("CREATE DATABASE seeded_damages");
            }
            String url = postgres.getJdbcUrl().replace("/" + postgres.getDatabaseName(), "/seeded_damages");
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
    @DisplayName("every seeded damage report is served by the admin search endpoint")
    void everySeededReportIsServed() throws Exception {
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM car_rental.damage_reports", Long.class);
        assertThat(total).isPositive();
        Long adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_rental.users WHERE username = 'admin'", Long.class);
        String token = tokenService.generateAccessToken(new UsernamePasswordAuthenticationToken("admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + Role.ADMIN.name()))), adminId);

        mockMvc.perform(get("/api/v1/admin/damages/search").param("size", "100")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(total.intValue()))
                .andExpect(jsonPath("$.content.length()").value(total.intValue()));
    }
}
