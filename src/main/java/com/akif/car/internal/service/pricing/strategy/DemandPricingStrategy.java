package com.akif.car.internal.service.pricing.strategy;

import com.akif.car.internal.config.PricingConfig;
import com.akif.car.api.CarBookingCalendar;
import com.akif.car.api.PriceModifier;
import com.akif.car.internal.dto.pricing.PricingContext;
import com.akif.car.internal.service.pricing.PricingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(5)
@RequiredArgsConstructor
public class DemandPricingStrategy implements PricingStrategy {

    private final PricingConfig config;
    private final CarBookingCalendar bookingCalendar;

    @Override
    public PriceModifier calculate(PricingContext context) {

        long overlappingRentals = bookingCalendar.countActiveBookings(
            context.carId(),
            context.startDate(),
            context.endDate()
        );

        int occupancyPercent = (int) Math.min(overlappingRentals * 10, 100);

        if (occupancyPercent > config.getDemand().getHigh().getThreshold()) {
            return PriceModifier.surcharge(
                getStrategyName(),
                config.getDemand().getHigh().getMultiplier(),
                String.format("High demand (%d%% occupancy)", occupancyPercent)
            );
        }

        if (occupancyPercent >= config.getDemand().getModerate().getThreshold()) {
            return PriceModifier.surcharge(
                getStrategyName(),
                config.getDemand().getModerate().getMultiplier(),
                String.format("Moderate demand (%d%% occupancy)", occupancyPercent)
            );
        }

        return PriceModifier.neutral(
            getStrategyName(),
            String.format("Normal demand (%d%% occupancy)", occupancyPercent)
        );
    }

    @Override
    public String getStrategyName() {
        return "Demand Pricing";
    }

    @Override
    public boolean isEnabled() {
        return config.getStrategy().isDemandEnabled();
    }

    @Override
    public int getOrder() {
        return 5;
    }
}
