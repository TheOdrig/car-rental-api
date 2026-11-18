package com.akif.payment.internal.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class InstallationIdentity {

    private final Supplier<String> source;
    private volatile String cached;

    @Autowired
    public InstallationIdentity(JdbcTemplate jdbcTemplate,
                                @Value("${spring.jpa.properties.hibernate.default_schema:car_rental}") String schema) {
        this(() -> jdbcTemplate.queryForObject(
                "SELECT installation_id::text FROM " + schema + ".installation_identity WHERE id = 1", String.class));
    }

    public InstallationIdentity(Supplier<String> source) {
        this.source = source;
    }

    public String id() {
        String value = cached;
        if (value == null) {
            value = source.get();
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Installation identity is missing");
            }
            cached = value;
        }
        return value;
    }
}
