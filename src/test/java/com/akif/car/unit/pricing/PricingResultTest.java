package com.akif.car.unit.pricing;

import com.akif.car.api.PriceModifier;
import com.akif.car.api.PricingResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PricingResult savings")
class PricingResultTest {

    private PricingResult result(String finalPrice, PriceModifier modifier) {
        return PricingResult.builder()
                .basePrice(new BigDecimal("100.00"))
                .rentalDays(3)
                .appliedModifiers(List.of(modifier))
                .combinedMultiplier(BigDecimal.ONE)
                .finalPrice(new BigDecimal(finalPrice))
                .build();
    }

    @Test
    @DisplayName("a discount is reported as savings")
    void discountIsSavings() {
        PricingResult discounted = result("270.00",
                PriceModifier.discount(com.akif.car.api.PricingStrategyCode.SEASON, "EarlyBooking", new BigDecimal("0.90"), "Early booking"));

        assertThat(discounted.totalSavings()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("a surcharge never produces negative savings")
    void surchargeIsNotNegativeSavings() {
        PricingResult surcharged = result("305.04",
                PriceModifier.surcharge(com.akif.car.api.PricingStrategyCode.SEASON, "Weekend", new BigDecimal("1.0168"), "Weekend"));

        assertThat(surcharged.totalSavings()).isEqualByComparingTo("0");
    }
}
