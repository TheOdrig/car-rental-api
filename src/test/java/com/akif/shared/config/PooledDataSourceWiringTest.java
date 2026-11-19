package com.akif.shared.config;

import com.akif.testsupport.TestcontainersConfiguration;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PooledDataSourceWiringTest {

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TestcontainersConfiguration.postgres().getJdbcUrl());
        registry.add("spring.datasource.username", () -> TestcontainersConfiguration.postgres().getUsername());
        registry.add("spring.datasource.password", () -> TestcontainersConfiguration.postgres().getPassword());
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JdbcConnectionDetails connectionDetails;

    @Test
    void primaryDataSourceIsTheHikariPool() {
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        assertThat(connectionDetails.getJdbcUrl()).isEqualTo(TestcontainersConfiguration.postgres().getJdbcUrl());
    }

    @Test
    void poolConnectionsCarryTheSessionSettings() {
        assertThat(jdbcTemplate.queryForObject("SHOW statement_timeout", String.class)).isEqualTo("30s");
        assertThat(jdbcTemplate.queryForObject("SHOW TimeZone", String.class)).isEqualTo("UTC");
        assertThat(jdbcTemplate.queryForObject("SHOW lock_timeout", String.class)).isEqualTo("5s");
    }

    @Test
    void flywayMigratedThroughItsOwnDataSource() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM car_rental.flyway_schema_history WHERE success", Integer.class);
        assertThat(applied).isPositive();
    }
}
