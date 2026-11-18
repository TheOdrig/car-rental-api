package com.akif.testsupport;

import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withCommand("postgres", "-c", "max_connections=300", "-c", "fsync=off");

    static {
        POSTGRES.start();
    }

    public static PostgreSQLContainer<?> postgres() {
        return POSTGRES;
    }

    @Bean
    JdbcConnectionDetails postgresConnectionDetails() {
        return new JdbcConnectionDetails() {

            @Override
            public String getUsername() {
                return POSTGRES.getUsername();
            }

            @Override
            public String getPassword() {
                return POSTGRES.getPassword();
            }

            @Override
            public String getJdbcUrl() {
                return POSTGRES.getJdbcUrl();
            }
        };
    }
}
