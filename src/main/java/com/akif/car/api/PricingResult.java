package com.akif.car.api;

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
        private int scale = 2;

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

        public Builder scale(int scale) {
            this.scale = scale;
            return this;
        }

        public Builder finalPrice(BigDecimal finalPrice) {
            this.finalPrice = finalPrice;
            return this;
        }

        public PricingResult build() {
            BigDecimal baseTotalPrice = basePrice.multiply(BigDecimal.valueOf(rentalDays));
            BigDecimal effectiveDailyPrice = rentalDays > 0 
                ? finalPrice.divide(BigDecimal.valueOf(rentalDays), scale, RoundingMode.HALF_UP)
                : finalPrice;
            BigDecimal totalSavings = baseTotalPrice.subtract(finalPrice).max(BigDecimal.ZERO);

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
