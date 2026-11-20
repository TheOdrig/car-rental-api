package com.akif.car.internal.service.pricing.strategy;

import com.akif.car.internal.config.PricingConfig;
import com.akif.car.api.PriceModifier;
import com.akif.car.api.PricingStrategyCode;
import com.akif.car.internal.dto.pricing.PricingContext;
import com.akif.car.internal.service.pricing.PricingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DurationDiscountStrategy implements PricingStrategy {

    private final PricingConfig config;

    @Override
    public PriceModifier calculate(PricingContext context) {
        int rentalDays = context.rentalDays();

        if (rentalDays >= config.duration().tier3().days()) {
            return PriceModifier.discount(getCode(), getStrategyName(),
                config.duration().tier3().multiplier(),
                String.format("Long-term rental discount (%d+ days)", 
                    config.duration().tier3().days())
            );
        }

        if (rentalDays >= config.duration().tier2().days()) {
            return PriceModifier.discount(getCode(), getStrategyName(),
                config.duration().tier2().multiplier(),
                String.format("Extended rental discount (%d-%d days)",
                    config.duration().tier2().days(),
                    config.duration().tier3().days() - 1)
            );
        }

        if (rentalDays >= config.duration().tier1().days()) {
            return PriceModifier.discount(getCode(), getStrategyName(),
                config.duration().tier1().multiplier(),
                String.format("Weekly rental discount (%d-%d days)",
                    config.duration().tier1().days(),
                    config.duration().tier2().days() - 1)
            );
        }

        return PriceModifier.neutral(getCode(), getStrategyName(), "No duration discount");
    }

    @Override
    public PricingStrategyCode getCode() {
        return PricingStrategyCode.DURATION_DISCOUNT;
    }

    @Override
    public String getStrategyName() {
        return "Duration Discount";
    }

    @Override
    public boolean isEnabled() {
        return config.strategy().durationEnabled();
    }

    @Override
    public int getOrder() {
        return 3;
    }
}
