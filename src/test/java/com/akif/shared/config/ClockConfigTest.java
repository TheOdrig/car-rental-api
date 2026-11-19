package com.akif.shared.config;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClockConfig")
class ClockConfigTest {

    private final ClockConfig config = new ClockConfig();

    @Test
    @DisplayName("exposes the system clock in UTC")
    void exposesSystemClock() {
        assertThat(config.clock().getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("the test JVM runs in UTC so database defaults and application clocks agree")
    void jvmRunsInUtc() {
        assertThat(java.util.TimeZone.getDefault().getID()).isIn("UTC", "Etc/UTC", "GMT");
    }

    @Test
    @DisplayName("auditing timestamps follow the injected clock")
    void auditingUsesInjectedClock() {
        Clock fixed = Clock.fixed(Instant.parse("2026-03-01T10:15:30Z"), ZoneOffset.UTC);

        DateTimeProvider provider = config.auditingDateTimeProvider(fixed);

        assertThat(provider.getNow()).contains(LocalDateTime.of(2026, 3, 1, 10, 15, 30).toInstant(ZoneOffset.UTC));
    }
}
