package com.akif.car.internal.service.pricing.strategy;

import com.akif.car.api.CarBooking;
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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
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

        int occupancyPercent = context.fleetOccupancyPercent() != null
            ? context.fleetOccupancyPercent()
            : fleetOccupancyPercent(context);

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
    public PricingContext enrich(PricingContext context) {
        if (context.fleetOccupancyPercent() != null) {
            return context;
        }
        return context.withFleetOccupancyPercent(fleetOccupancyPercent(context));
    }

    private int fleetOccupancyPercent(PricingContext context) {
        List<Long> bookableIds = carRepository.findBookableCarIds(
            Arrays.asList(CarStatusType.getUnavailableStatuses())
        );
        if (bookableIds.isEmpty()) {
            return 0;
        }
        Set<Long> bookable = new HashSet<>(bookableIds);
        long days = ChronoUnit.DAYS.between(context.startDate(), context.endDate()) + 1;
        if (days <= 0) {
            return 0;
        }
        List<CarBooking> bookings = bookingCalendar.findCommittedBookings(context.startDate(), context.endDate());
        long occupiedCarDays = 0;
        for (LocalDate day = context.startDate(); !day.isAfter(context.endDate()); day = day.plusDays(1)) {
            LocalDate current = day;
            occupiedCarDays += bookings.stream()
                .filter(booking -> bookable.contains(booking.carId()))
                .filter(booking -> booking.bookingId() == null || !booking.bookingId().equals(context.excludedBookingId()))
                .filter(booking -> !current.isBefore(booking.startDate()) && !current.isAfter(booking.endDate()))
                .map(CarBooking::carId)
                .distinct()
                .count();
        }
        return (int) Math.min(occupiedCarDays * 100 / (days * bookable.size()), 100);
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
