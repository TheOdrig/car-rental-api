package com.akif.rental.unit.penalty;

import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.config.PenaltyConfig;
import com.akif.rental.internal.dto.penalty.PenaltyResult;
import com.akif.rental.internal.service.penalty.impl.PenaltyCalculationServiceImpl;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.time.LateHours;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Penalty tiers follow the configured hour limits")
class PenaltyTierConfigTest {

    private static final LocalDate END = LocalDate.of(2026, 11, 17);

    private Rental rental() {
        return Rental.builder().id(1L).endDate(END).dailyPrice(new BigDecimal("500.00"))
                .currency(CurrencyType.TRY).build();
    }

    private PenaltyResult penalty(PenaltyConfig config, long hoursLate) {
        Instant returnTime = LateHours.dueAt(END).plusSeconds(hoursLate * 3600 - 1800);
        return new PenaltyCalculationServiceImpl(config).calculatePenalty(rental(), returnTime);
    }

    @Test
    @DisplayName("a lower hourly tier limit moves a late return to the daily rate sooner")
    void lowerHourlyLimit() {
        PenaltyConfig config = new PenaltyConfig(0, new BigDecimal("0.10"), new BigDecimal("1.50"),
                new BigDecimal("5.0"), 24, 3, 24);

        assertThat(penalty(config, 3).penaltyAmount()).isEqualByComparingTo("150.00");
        assertThat(penalty(config, 4).penaltyAmount()).isEqualByComparingTo("750.00");
    }

    @Test
    @DisplayName("the default limits keep six hourly hours and a flat first day")
    void defaultLimits() {
        PenaltyConfig config = new PenaltyConfig(0, new BigDecimal("0.10"), new BigDecimal("1.50"),
                new BigDecimal("5.0"), 24, 6, 24);

        assertThat(penalty(config, 6).penaltyAmount()).isEqualByComparingTo("300.00");
        assertThat(penalty(config, 7).penaltyAmount()).isEqualByComparingTo("750.00");
    }
}
