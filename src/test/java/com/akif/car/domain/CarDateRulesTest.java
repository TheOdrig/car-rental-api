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
        assertThat(car.isOldAt(LocalDate.of(2027, 1, 1))).isTrue();
        assertThat(car.isOldAt(TODAY)).isFalse();
        assertThat(car.isNewAt(LocalDate.of(2016, 6, 1))).isTrue();
    }

    @Test
    @DisplayName("age is zero when the production year is unknown")
    void ageIsZeroWithoutProductionYear() {
        assertThat(Car.builder().build().ageAt(TODAY)).isZero();
    }

    @Test
    @DisplayName("a date on the deadline is not yet expired, the day after is")
    void expiryBoundary() {
        Car car = Car.builder()
                .nextServiceDate(TODAY)
                .insuranceExpiryDate(TODAY)
                .inspectionExpiryDate(TODAY.plusDays(1))
                .build();

        assertThat(car.needsServiceOn(TODAY)).isFalse();
        assertThat(car.needsServiceOn(TODAY.plusDays(1))).isTrue();
        assertThat(car.isInsuranceExpiredOn(TODAY)).isFalse();
        assertThat(car.isInsuranceExpiredOn(TODAY.plusDays(1))).isTrue();
        assertThat(car.hasExpiredDocumentsOn(TODAY.plusDays(1))).isTrue();
        assertThat(car.isInspectionExpiredOn(TODAY.plusDays(1))).isFalse();
    }

    @Test
    @DisplayName("missing dates never count as expired")
    void missingDatesAreNotExpired() {
        Car car = Car.builder().build();

        assertThat(car.needsServiceOn(TODAY)).isFalse();
        assertThat(car.hasExpiredDocumentsOn(TODAY)).isFalse();
    }
}
