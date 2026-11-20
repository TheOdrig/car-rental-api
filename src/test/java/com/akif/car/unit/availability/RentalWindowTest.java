package com.akif.car.unit.availability;

import com.akif.car.internal.exception.AvailabilityValidationException;
import com.akif.car.internal.service.availability.RentalWindow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RentalWindow")
class RentalWindowTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 20);

    @Test
    @DisplayName("accepts a single day and a range of exactly the maximum length")
    void acceptsBoundaries() {
        assertThatCode(() -> RentalWindow.validate(TODAY, TODAY, TODAY, 30)).doesNotThrowAnyException();
        assertThatCode(() -> RentalWindow.validate(TODAY, TODAY.plusDays(29), TODAY, 30)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects a start date in the past")
    void rejectsPast() {
        assertThatThrownBy(() -> RentalWindow.validate(TODAY.minusDays(1), TODAY, TODAY, 30))
                .isInstanceOf(AvailabilityValidationException.class);
    }

    @Test
    @DisplayName("rejects an end date before the start date")
    void rejectsReversed() {
        assertThatThrownBy(() -> RentalWindow.validate(TODAY.plusDays(2), TODAY.plusDays(1), TODAY, 30))
                .isInstanceOf(AvailabilityValidationException.class);
    }

    @Test
    @DisplayName("rejects ranges longer than the maximum, including ones beyond int")
    void rejectsTooLong() {
        assertThatThrownBy(() -> RentalWindow.validate(TODAY, TODAY.plusDays(30), TODAY, 30))
                .isInstanceOf(AvailabilityValidationException.class);
        assertThatThrownBy(() -> RentalWindow.validate(TODAY, LocalDate.MAX, TODAY, 30))
                .isInstanceOf(AvailabilityValidationException.class);
    }
}
