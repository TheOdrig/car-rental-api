package com.akif.car.internal.service.pricing;

import com.akif.car.api.PriceModifier;
import com.akif.car.internal.dto.pricing.PricingContext;

public interface PricingStrategy {

    PriceModifier calculate(PricingContext context);

    String getStrategyName();

    boolean isEnabled();

    int getOrder();
}
