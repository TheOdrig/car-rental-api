package com.akif.rental.domain;

import com.akif.rental.domain.model.RentalDatePolicy;
import com.akif.rental.internal.exception.RentalValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Rental date policy")
class RentalDatePolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    private final RentalDatePolicy policy = new RentalDatePolicy(180, 30);

    @Test
    @DisplayName("accepts today as the start and a rental of exactly the maximum inclusive length")
    void acceptsBoundaries() {
        assertThatCode(() -> policy.validate(TODAY, TODAY.plusDays(29), TODAY)).doesNotThrowAnyException();
        assertThatCode(() -> policy.validate(TODAY.plusDays(180), TODAY.plusDays(181), TODAY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects a start in the past relative to the given day")
    void rejectsPastStart() {
        assertThatThrownBy(() -> policy.validate(TODAY.minusDays(1), TODAY.plusDays(2), TODAY))
                .isInstanceOf(RentalValidationException.class)
                .hasMessageContaining("past");
    }

    @Test
    @DisplayName("rejects an end that is not after the start")
    void rejectsNonPositiveRange() {
        assertThatThrownBy(() -> policy.validate(TODAY, TODAY, TODAY))
                .isInstanceOf(RentalValidationException.class)
                .hasMessageContaining("after start date");
    }

    @Test
    @DisplayName("rejects a start beyond the lead time and a rental beyond the maximum length")
    void rejectsLimits() {
        assertThatThrownBy(() -> policy.validate(TODAY.plusDays(181), TODAY.plusDays(183), TODAY))
                .hasMessageContaining("180 days ahead");
        assertThatThrownBy(() -> policy.validate(TODAY, TODAY.plusDays(30), TODAY))
                .hasMessageContaining("30 days");
    }

    @Test
    @DisplayName("takes its limits from the configured values")
    void honoursConfiguredLimits() {
        RentalDatePolicy relaxed = new RentalDatePolicy(365, 60);

        assertThatCode(() -> relaxed.validate(TODAY.plusDays(300), TODAY.plusDays(340), TODAY))
                .doesNotThrowAnyException();
        assertThat(com.akif.shared.time.RentalDays.inclusiveDays(TODAY, TODAY.plusDays(4))).isEqualTo(5);
    }
}
