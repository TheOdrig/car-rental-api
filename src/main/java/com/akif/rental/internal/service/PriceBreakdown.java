package com.akif.rental.internal.service;

import com.akif.car.api.PriceModifier;
import com.akif.car.api.PricingResult;
import com.akif.rental.api.PriceAdjustment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

final class PriceBreakdown {

    static final String PRICE_CAP_CODE = "PRICE_LIMIT";
    static final String PRICE_CAP_LABEL = "Daily price limit applied";

    private PriceBreakdown() {
    }

    static List<PriceAdjustment> adjustments(PricingResult pricing, int decimalPlaces) {
        List<PriceAdjustment> adjustments = new ArrayList<>();
        BigDecimal running = pricing.baseTotalPrice();
        BigDecimal roundedRunning = running.setScale(decimalPlaces, RoundingMode.HALF_UP);
        for (PriceModifier modifier : pricing.appliedModifiers()) {
            running = running.multiply(modifier.multiplier());
            BigDecimal roundedNext = running.setScale(decimalPlaces, RoundingMode.HALF_UP);
            adjustments.add(new PriceAdjustment(modifier.strategyName(), modifier.description(),
                    modifier.multiplier(), roundedNext.subtract(roundedRunning)));
            roundedRunning = roundedNext;
        }
        BigDecimal finalPrice = pricing.finalPrice().setScale(decimalPlaces, RoundingMode.HALF_UP);
        BigDecimal capDelta = finalPrice.subtract(roundedRunning);
        if (capDelta.signum() != 0) {
            adjustments.add(new PriceAdjustment(PRICE_CAP_CODE, PRICE_CAP_LABEL, null, capDelta));
        }
        return adjustments;
    }
}
