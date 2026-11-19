package com.akif.rental.unit;

import com.akif.rental.internal.config.RentalProperties;
import com.akif.rental.internal.service.PaymentWindow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Payment deadline")
class PaymentDeadlineTest {

    private static final Instant NOW = Instant.parse("2026-11-19T10:00:00Z");

    private final RentalProperties properties = new RentalProperties();
    private final PaymentWindow window = new PaymentWindow(Clock.fixed(NOW, ZoneOffset.UTC), properties);

    @Test
    @DisplayName("a same-day rental is due one grace period after approval")
    void sameDay() {
        Instant due = window.dueBy(LocalDate.parse("2026-11-19"), Instant.parse("2026-11-19T09:50:00Z"));

        assertThat(due).isEqualTo(Instant.parse("2026-11-19T21:50:00Z"));
        assertThat(window.isOverdue(LocalDate.parse("2026-11-19"), Instant.parse("2026-11-19T09:50:00Z"))).isFalse();
    }

    @Test
    @DisplayName("a next-day rental approved late in the evening still gets the full grace period")
    void nextDay() {
        Instant due = window.dueBy(LocalDate.parse("2026-11-20"), Instant.parse("2026-11-19T20:00:00Z"));

        assertThat(due).isEqualTo(Instant.parse("2026-11-20T08:00:00Z"));
    }

    @Test
    @DisplayName("a next-day rental approved in the morning is due when the start day begins")
    void nextDayApprovedEarly() {
        Instant due = window.dueBy(LocalDate.parse("2026-11-20"), Instant.parse("2026-11-19T08:00:00Z"));

        assertThat(due).isEqualTo(Instant.parse("2026-11-20T00:00:00Z"));
    }

    @Test
    @DisplayName("a far-future rental is due when the start day begins")
    void farFuture() {
        Instant due = window.dueBy(LocalDate.parse("2027-02-01"), Instant.parse("2026-11-19T09:50:00Z"));

        assertThat(due).isEqualTo(Instant.parse("2027-02-01T00:00:00Z"));
        assertThat(window.isOverdue(LocalDate.parse("2027-02-01"), Instant.parse("2026-11-19T09:50:00Z"))).isFalse();
    }

    @Test
    @DisplayName("a rental without an approval time falls back to the start of the start day")
    void withoutApprovalTime() {
        assertThat(window.dueBy(LocalDate.parse("2026-11-19"), null)).isEqualTo(Instant.parse("2026-11-19T00:00:00Z"));
        assertThat(window.isOverdue(LocalDate.parse("2026-11-19"), null)).isTrue();
    }

    @Test
    @DisplayName("a grace period shorter than one hour is raised to one hour")
    void graceHoursAreBounded() {
        properties.setPaymentGraceHours(0);
        properties.validate();

        assertThat(properties.getPaymentGraceHours()).isEqualTo(1);
    }
}
