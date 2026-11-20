package com.akif.rental.unit;

import com.akif.rental.internal.config.RentalProperties;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RentalProperties validation")
class RentalPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private RentalProperties properties(int paymentWindowDays, int paymentGraceHours, int noShowDays) {
        return new RentalProperties(30, 180, 3, 24, paymentWindowDays, paymentGraceHours, noShowDays);
    }

    @Test
    @DisplayName("accepts the defaults")
    void acceptsDefaults() {
        assertThat(validator.validate(properties(5, 12, 1))).isEmpty();
    }

    @Test
    @DisplayName("rejects a no-show window that would outlive the 7-day authorisation")
    void rejectsNoShowBeyondAuthorisationWindow() {
        assertThatThrownBy(() -> properties(5, 12, 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rental.no-show-days");
    }

    @Test
    @DisplayName("allows a longer no-show window when the payment window is shorter")
    void longerWithAShorterPaymentWindow() {
        assertThat(validator.validate(properties(3, 12, 3))).isEmpty();
    }

    @Test
    @DisplayName("rejects a no-show window below one day")
    void rejectsZeroNoShowDays() {
        assertThat(validator.validate(properties(5, 12, 0))).isNotEmpty();
    }

    @Test
    @DisplayName("rejects a payment grace period outside 1..72 hours")
    void rejectsGraceOutsideBounds() {
        assertThat(validator.validate(properties(5, 0, 1))).isNotEmpty();
        assertThat(validator.validate(properties(5, 500, 1))).isNotEmpty();
    }
}
