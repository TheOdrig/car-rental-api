package com.akif.shared.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClockConfig")
class ClockConfigTest {

    private final ClockConfig config = new ClockConfig();

    @Test
    @DisplayName("exposes the system clock in the default zone")
    void exposesSystemClock() {
        assertThat(config.clock().getZone()).isEqualTo(Clock.systemDefaultZone().getZone());
    }

    @Test
    @DisplayName("auditing timestamps follow the injected clock")
    void auditingUsesInjectedClock() {
        Clock fixed = Clock.fixed(Instant.parse("2026-03-01T10:15:30Z"), ZoneOffset.UTC);

        DateTimeProvider provider = config.auditingDateTimeProvider(fixed);

        assertThat(provider.getNow()).contains(LocalDateTime.of(2026, 3, 1, 10, 15, 30));
    }
}
