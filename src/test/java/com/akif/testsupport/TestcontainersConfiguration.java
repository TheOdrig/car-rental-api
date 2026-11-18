package com.akif.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withCommand("postgres", "-c", "max_connections=300", "-c", "fsync=off");

    static {
        POSTGRES.start();
    }

    public static PostgreSQLContainer<?> postgres() {
        return POSTGRES;
    }

    @Bean(destroyMethod = "")
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return POSTGRES;
    }
}
