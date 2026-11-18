package com.akif.car.internal.service.pricing.strategy;

import com.akif.car.internal.config.PricingConfig;
import com.akif.car.api.PriceModifier;
import com.akif.car.internal.dto.pricing.PricingContext;
import com.akif.car.internal.service.pricing.PricingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
@RequiredArgsConstructor
public class DurationDiscountStrategy implements PricingStrategy {

    private final PricingConfig config;

    @Override
    public PriceModifier calculate(PricingContext context) {
        int rentalDays = context.rentalDays();

        if (rentalDays >= config.getDuration().getTier3().getDays()) {
            return PriceModifier.discount(
                getStrategyName(),
                config.getDuration().getTier3().getMultiplier(),
                String.format("Long-term rental discount (%d+ days)", 
                    config.getDuration().getTier3().getDays())
            );
        }

        if (rentalDays >= config.getDuration().getTier2().getDays()) {
            return PriceModifier.discount(
                getStrategyName(),
                config.getDuration().getTier2().getMultiplier(),
                String.format("Extended rental discount (%d-%d days)",
                    config.getDuration().getTier2().getDays(),
                    config.getDuration().getTier3().getDays() - 1)
            );
        }

        if (rentalDays >= config.getDuration().getTier1().getDays()) {
            return PriceModifier.discount(
                getStrategyName(),
                config.getDuration().getTier1().getMultiplier(),
                String.format("Weekly rental discount (%d-%d days)",
                    config.getDuration().getTier1().getDays(),
                    config.getDuration().getTier2().getDays() - 1)
            );
        }

        return PriceModifier.neutral(getStrategyName(), "No duration discount");
    }

    @Override
    public String getStrategyName() {
        return "Duration Discount";
    }

    @Override
    public boolean isEnabled() {
        return config.getStrategy().isDurationEnabled();
    }

    @Override
    public int getOrder() {
        return 3;
    }
}
