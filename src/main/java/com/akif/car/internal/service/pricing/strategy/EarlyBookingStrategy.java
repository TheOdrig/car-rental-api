package com.akif.car.internal.service.pricing.strategy;

import com.akif.car.internal.config.PricingConfig;
import com.akif.car.api.PriceModifier;
import com.akif.car.internal.dto.pricing.PricingContext;
import com.akif.car.internal.service.pricing.PricingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@RequiredArgsConstructor
public class EarlyBookingStrategy implements PricingStrategy {

    private final PricingConfig config;

    @Override
    public PriceModifier calculate(PricingContext context) {
        int leadTimeDays = context.leadTimeDays();

        if (leadTimeDays >= config.earlyBooking().tier1().days()) {
            return PriceModifier.discount(
                getStrategyName(),
                config.earlyBooking().tier1().multiplier(),
                String.format("Early booking discount (%d+ days advance)", 
                    config.earlyBooking().tier1().days())
            );
        }

        if (leadTimeDays >= config.earlyBooking().tier2().days()) {
            return PriceModifier.discount(
                getStrategyName(),
                config.earlyBooking().tier2().multiplier(),
                String.format("Early booking discount (%d-%d days advance)",
                    config.earlyBooking().tier2().days(),
                    config.earlyBooking().tier1().days() - 1)
            );
        }

        if (leadTimeDays >= config.earlyBooking().tier3().days()) {
            return PriceModifier.discount(
                getStrategyName(),
                config.earlyBooking().tier3().multiplier(),
                String.format("Early booking discount (%d-%d days advance)",
                    config.earlyBooking().tier3().days(),
                    config.earlyBooking().tier2().days() - 1)
            );
        }

        return PriceModifier.neutral(getStrategyName(), "No early booking discount");
    }

    @Override
    public String getStrategyName() {
        return "Early Booking";
    }

    @Override
    public boolean isEnabled() {
        return config.strategy().earlyBookingEnabled();
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
