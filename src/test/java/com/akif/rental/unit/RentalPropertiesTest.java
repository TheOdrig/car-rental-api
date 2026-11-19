package com.akif.rental.unit;

import com.akif.rental.internal.config.RentalProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RentalProperties no-show days")
class RentalPropertiesTest {

    @Test
    @DisplayName("defaults to one day")
    void defaultsToOneDay() {
        RentalProperties properties = new RentalProperties();
        properties.validate();

        assertThat(properties.getNoShowDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("is clamped so the hold is released before the 7-day authorisation expires")
    void clampedToTheAuthorisationWindow() {
        RentalProperties properties = new RentalProperties();
        properties.setPaymentWindowDays(5);
        properties.setNoShowDays(4);
        properties.validate();

        assertThat(properties.getNoShowDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("may be longer when the payment window is shorter")
    void longerWithAShorterPaymentWindow() {
        RentalProperties properties = new RentalProperties();
        properties.setPaymentWindowDays(3);
        properties.setNoShowDays(3);
        properties.validate();

        assertThat(properties.getNoShowDays()).isEqualTo(3);
    }

    @Test
    @DisplayName("is never below one day")
    void neverBelowOne() {
        RentalProperties properties = new RentalProperties();
        properties.setNoShowDays(0);
        properties.validate();

        assertThat(properties.getNoShowDays()).isEqualTo(1);
    }
}
