package com.akif.car.internal.dto.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record PricingResult(
    BigDecimal basePrice,
    BigDecimal baseTotalPrice,
    List<PriceModifier> appliedModifiers,
    BigDecimal combinedMultiplier,
    BigDecimal finalPrice,
    BigDecimal effectiveDailyPrice,
    BigDecimal totalSavings,
    int rentalDays
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BigDecimal basePrice;
        private int rentalDays;
        private List<PriceModifier> appliedModifiers;
        private BigDecimal combinedMultiplier;
        private BigDecimal finalPrice;

        public Builder basePrice(BigDecimal basePrice) {
            this.basePrice = basePrice;
            return this;
        }

        public Builder rentalDays(int rentalDays) {
            this.rentalDays = rentalDays;
            return this;
        }

        public Builder appliedModifiers(List<PriceModifier> appliedModifiers) {
            this.appliedModifiers = appliedModifiers;
            return this;
        }

        public Builder combinedMultiplier(BigDecimal combinedMultiplier) {
            this.combinedMultiplier = combinedMultiplier;
            return this;
        }

        public Builder finalPrice(BigDecimal finalPrice) {
            this.finalPrice = finalPrice;
            return this;
        }

        public PricingResult build() {
            BigDecimal baseTotalPrice = basePrice.multiply(BigDecimal.valueOf(rentalDays));
            BigDecimal effectiveDailyPrice = rentalDays > 0 
                ? finalPrice.divide(BigDecimal.valueOf(rentalDays), 2, RoundingMode.HALF_UP)
                : finalPrice;
            BigDecimal totalSavings = baseTotalPrice.subtract(finalPrice);

            return new PricingResult(
                basePrice,
                baseTotalPrice,
                appliedModifiers,
                combinedMultiplier,
                finalPrice,
                effectiveDailyPrice,
                totalSavings,
                rentalDays
            );
        }
    }
}
