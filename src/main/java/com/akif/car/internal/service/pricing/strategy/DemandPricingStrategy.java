package com.akif.car.internal.service.pricing.strategy;

import com.akif.car.api.CarBookingCalendar;
import com.akif.car.api.CarStatusType;
import com.akif.car.api.PriceModifier;
import com.akif.car.internal.config.PricingConfig;
import com.akif.car.internal.dto.pricing.PricingContext;
import com.akif.car.internal.repository.CarRepository;
import com.akif.car.internal.service.pricing.PricingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Component
@Order(5)
@RequiredArgsConstructor
public class DemandPricingStrategy implements PricingStrategy {

    private final PricingConfig config;
    private final CarBookingCalendar bookingCalendar;
    private final CarRepository carRepository;

    @Override
    public PriceModifier calculate(PricingContext context) {

        int occupancyPercent = fleetOccupancyPercent(context);

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

    private int fleetOccupancyPercent(PricingContext context) {
        List<Long> bookableIds = carRepository.findBookableCarIds(
            Arrays.asList(CarStatusType.getUnavailableStatuses())
        );
        if (bookableIds.isEmpty()) {
            return 0;
        }
        Set<Long> bookedIds = bookingCalendar.findBookedCarIds(context.startDate(), context.endDate());
        long occupied = bookableIds.stream().filter(bookedIds::contains).count();
        return (int) Math.min(occupied * 100 / bookableIds.size(), 100);
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
