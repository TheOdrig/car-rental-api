package com.akif.shared.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RentalDays")
class RentalDaysTest {

    private static final LocalDate D = LocalDate.of(2026, 11, 20);

    @Test
    @DisplayName("counts both the first and the last day")
    void inclusiveDays() {
        assertThat(RentalDays.inclusiveDays(D, D)).isEqualTo(1);
        assertThat(RentalDays.inclusiveDays(D, D.plusDays(4))).isEqualTo(5);
        assertThat(RentalDays.inclusiveDays(D, LocalDate.of(2027, 2, 20))).isEqualTo(93);
    }

    @Test
    @DisplayName("daysBetween is exclusive and signed")
    void daysBetween() {
        assertThat(RentalDays.daysBetween(D, D.plusDays(7))).isEqualTo(7);
        assertThat(RentalDays.daysBetween(D, D)).isZero();
        assertThat(RentalDays.daysBetween(D, D.minusDays(2))).isEqualTo(-2);
    }

    @Test
    @DisplayName("elapsed days are zero before the start and inclusive afterwards")
    void elapsedInclusiveDays() {
        assertThat(RentalDays.elapsedInclusiveDays(D, D.minusDays(1))).isZero();
        assertThat(RentalDays.elapsedInclusiveDays(D, D)).isEqualTo(1);
        assertThat(RentalDays.elapsedInclusiveDays(D, D.plusDays(2))).isEqualTo(3);
    }

    @Test
    @DisplayName("late days round partial days up")
    void lateDays() {
        assertThat(RentalDays.lateDays(0)).isZero();
        assertThat(RentalDays.lateDays(-3)).isZero();
        assertThat(RentalDays.lateDays(1)).isEqualTo(1);
        assertThat(RentalDays.lateDays(24)).isEqualTo(1);
        assertThat(RentalDays.lateDays(25)).isEqualTo(2);
        assertThat(RentalDays.lateDays(48)).isEqualTo(2);
        assertThat(RentalDays.lateDays(49)).isEqualTo(3);
    }

    @Test
    @DisplayName("iterates every day inclusively and is empty for a reversed range")
    void eachDay() {
        assertThat(RentalDays.eachDay(D, D.plusDays(2)).toList()).isEqualTo(List.of(D, D.plusDays(1), D.plusDays(2)));
        assertThat(RentalDays.eachDay(D, D).toList()).containsExactly(D);
        assertThat(RentalDays.eachDay(D, D.minusDays(1)).toList()).isEmpty();
    }
}
