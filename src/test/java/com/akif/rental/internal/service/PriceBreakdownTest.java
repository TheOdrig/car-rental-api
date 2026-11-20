package com.akif.rental.internal.service;

import com.akif.car.api.PriceModifier;
import com.akif.car.api.PricingResult;
import com.akif.rental.api.PriceAdjustment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PriceBreakdown")
class PriceBreakdownTest {

    private static PricingResult pricing(BigDecimal basePrice, int days, List<PriceModifier> modifiers, BigDecimal finalPrice) {
        BigDecimal combined = modifiers.stream().map(PriceModifier::multiplier).reduce(BigDecimal.ONE, BigDecimal::multiply);
        return PricingResult.builder()
                .basePrice(basePrice)
                .rentalDays(days)
                .appliedModifiers(modifiers)
                .combinedMultiplier(combined)
                .finalPrice(finalPrice)
                .build();
    }

    @Test
    @DisplayName("records every modifier with a signed amount that sums to final minus base")
    void amountsSumToTheDifference() {
        List<PriceModifier> modifiers = List.of(
                PriceModifier.surcharge(com.akif.car.api.PricingStrategyCode.SEASON, "SEASON", new BigDecimal("1.15"), "Summer season"),
                PriceModifier.neutral(com.akif.car.api.PricingStrategyCode.WEEKEND, "WEEKEND", "No weekend days"),
                PriceModifier.discount(com.akif.car.api.PricingStrategyCode.DURATION_DISCOUNT, "DURATION", new BigDecimal("0.90"), "Rental discount"));
        PricingResult result = pricing(new BigDecimal("100.00"), 3, modifiers, new BigDecimal("310.50"));

        List<PriceAdjustment> adjustments = PriceBreakdown.adjustments(result, 2);

        assertThat(adjustments).extracting(PriceAdjustment::code).containsExactly("SEASON", "WEEKEND", "DURATION_DISCOUNT");
        assertThat(adjustments.get(0).amount()).isEqualByComparingTo("45.00");
        assertThat(adjustments.get(1).amount()).isEqualByComparingTo("0.00");
        assertThat(adjustments.get(2).amount()).isEqualByComparingTo("-34.50");
        assertThat(PriceAdjustment.changingThePrice(adjustments)).extracting(PriceAdjustment::code)
                .containsExactly("SEASON", "DURATION_DISCOUNT");
        assertThat(adjustments.stream().map(PriceAdjustment::amount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("10.50");
    }

    @Test
    @DisplayName("adds a price limit entry when the daily price cap changed the total")
    void capIsListed() {
        List<PriceModifier> modifiers = List.of(
                PriceModifier.surcharge(com.akif.car.api.PricingStrategyCode.DEMAND, "DEMAND", new BigDecimal("1.50"), "High demand"));
        PricingResult result = pricing(new BigDecimal("100.00"), 2, modifiers, new BigDecimal("260.00"));

        List<PriceAdjustment> adjustments = PriceBreakdown.adjustments(result, 2);

        assertThat(adjustments).extracting(PriceAdjustment::code).containsExactly("DEMAND", "PRICE_LIMIT");
        assertThat(adjustments.get(1).amount()).isEqualByComparingTo("-40.00");
        assertThat(adjustments.get(1).multiplier()).isNull();
    }

    @Test
    @DisplayName("changes no price when every modifier is neutral")
    void emptyWhenNeutral() {
        PricingResult result = pricing(new BigDecimal("100.00"), 2,
                List.of(PriceModifier.neutral(com.akif.car.api.PricingStrategyCode.SEASON, "SEASON", "Regular season")), new BigDecimal("200.00"));

        assertThat(PriceAdjustment.changingThePrice(PriceBreakdown.adjustments(result, 2))).isEmpty();
    }
}
