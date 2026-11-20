package com.akif.car.api;

import java.math.BigDecimal;

public record PriceModifier(
    PricingStrategyCode code,
    String strategyName,
    BigDecimal multiplier,
    String description,
    boolean isDiscount
) {

    public static PriceModifier discount(PricingStrategyCode code, String strategyName, BigDecimal multiplier,
                                         String description) {
        return new PriceModifier(code, strategyName, multiplier, description, true);
    }

    public static PriceModifier surcharge(PricingStrategyCode code, String strategyName, BigDecimal multiplier,
                                          String description) {
        return new PriceModifier(code, strategyName, multiplier, description, false);
    }

    public static PriceModifier neutral(PricingStrategyCode code, String strategyName, String description) {
        return new PriceModifier(code, strategyName, BigDecimal.ONE, description, false);
    }
}
