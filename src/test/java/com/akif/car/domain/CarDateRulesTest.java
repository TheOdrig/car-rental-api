package com.akif.car.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Car date-dependent rules")
class CarDateRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    @Test
    @DisplayName("age is the difference between the reference year and the production year")
    void ageUsesReferenceDate() {
        Car car = Car.builder().productionYear(2016).build();

        assertThat(car.ageAt(LocalDate.of(2026, 1, 1))).isEqualTo(10);
        assertThat(car.ageAt(LocalDate.of(2016, 12, 31))).isZero();
    }

    @Test
    @DisplayName("age is zero when the production year is unknown")
    void ageIsZeroWithoutProductionYear() {
        assertThat(Car.builder().build().ageAt(TODAY)).isZero();
    }
}
