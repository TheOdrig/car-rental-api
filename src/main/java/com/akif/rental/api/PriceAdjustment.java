package com.akif.rental.api;

import java.math.BigDecimal;
import java.util.List;

public record PriceAdjustment(String code, String label, BigDecimal multiplier, BigDecimal amount) {

    public static List<PriceAdjustment> changingThePrice(List<PriceAdjustment> all) {
        return all == null ? null : all.stream().filter(adjustment -> adjustment.amount().signum() != 0).toList();
    }
}
