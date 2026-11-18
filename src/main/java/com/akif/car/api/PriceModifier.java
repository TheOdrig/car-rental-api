package com.akif.car.api;

import java.math.BigDecimal;

public record PriceModifier(
    String strategyName,
    BigDecimal multiplier,
    String description,
    boolean isDiscount
) {

    public static PriceModifier discount(String strategyName, BigDecimal multiplier, String description) {
        return new PriceModifier(strategyName, multiplier, description, true);
    }

    public static PriceModifier surcharge(String strategyName, BigDecimal multiplier, String description) {
        return new PriceModifier(strategyName, multiplier, description, false);
    }

    public static PriceModifier neutral(String strategyName, String description) {
        return new PriceModifier(strategyName, BigDecimal.ONE, description, false);
    }
}
