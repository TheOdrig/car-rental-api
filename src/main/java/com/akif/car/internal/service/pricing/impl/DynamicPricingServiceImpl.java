package com.akif.car.internal.service.pricing.impl;

import com.akif.car.api.DynamicPricingService;
import java.time.Clock;

import com.akif.car.api.PriceModifier;
import com.akif.car.internal.dto.pricing.PricingContext;
import com.akif.car.api.PricingResult;
import com.akif.car.internal.dto.pricing.*;
import com.akif.car.internal.service.pricing.*;
import com.akif.car.internal.config.PricingConfig;
import com.akif.car.internal.exception.CarNotFoundException;
import com.akif.car.domain.Car;
import com.akif.shared.enums.CurrencyType;
import com.akif.car.internal.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import com.akif.shared.time.RentalDays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicPricingServiceImpl implements DynamicPricingService, PricingStrategyCatalog, BatchPricing {

    private final Clock clock;

    private final List<PricingStrategy> allStrategies;
    private final CarRepository carRepository;
    private final PricingConfig config;

    @Override
    public PricingResult calculatePrice(Long carId, LocalDate startDate, LocalDate endDate, LocalDate bookingDate) {
        return calculatePrice(carId, startDate, endDate, bookingDate, null);
    }

    @Override
    public PricingResult calculatePrice(Long carId, LocalDate startDate, LocalDate endDate, LocalDate bookingDate,
                                        Long excludedBookingId) {
        log.debug("Calculating price for car: {}, dates: {} to {}, booking: {}", 
            carId, startDate, endDate, bookingDate);

        Car car = carRepository.findByIdAndIsDeletedFalse(carId)
            .orElseThrow(() -> new CarNotFoundException(carId));

        PricingContext context = createContext(car, startDate, endDate, bookingDate, excludedBookingId);
        
        return calculatePriceWithContext(context, car.getCurrencyType());
    }

    @Override
    public Map<Long, PricingResult> priceCars(Collection<Car> cars, LocalDate startDate, LocalDate endDate,
                                              LocalDate bookingDate) {
        Map<Long, PricingResult> results = new LinkedHashMap<>();
        if (cars.isEmpty()) {
            return results;
        }
        List<PricingStrategy> strategies = getEnabledStrategies();
        PricingContext shared = createContext(cars.iterator().next(), startDate, endDate, bookingDate, null);
        for (PricingStrategy strategy : strategies) {
            shared = strategy.enrich(shared);
        }
        Integer occupancy = shared.fleetOccupancyPercent();
        for (Car car : cars) {
            PricingContext context = createContext(car, startDate, endDate, bookingDate, null)
                .withFleetOccupancyPercent(occupancy);
            results.put(car.getId(), calculatePriceWithContext(context, car.getCurrencyType()));
        }
        return results;
    }

    @Override
    public PricingResult previewPrice(Long carId, LocalDate startDate, LocalDate endDate) {
        return calculatePrice(carId, startDate, endDate, LocalDate.now(clock));
    }

    @Override
    public List<PricingStrategy> getEnabledStrategies() {
        return allStrategies.stream()
            .filter(PricingStrategy::isEnabled)
            .sorted(Comparator.comparingInt(PricingStrategy::getOrder))
            .toList();
    }

    private PricingResult calculatePriceWithContext(PricingContext context, CurrencyType currency) {
        int places = currency.getDecimalPlaces();
        List<PricingStrategy> enabledStrategies = getEnabledStrategies();
        
        log.debug("Applying {} enabled strategies", enabledStrategies.size());

        List<PriceModifier> appliedModifiers = new ArrayList<>();
        BigDecimal combinedMultiplier = BigDecimal.ONE;

        for (PricingStrategy strategy : enabledStrategies) {
            PriceModifier modifier = strategy.calculate(context);
            appliedModifiers.add(modifier);
            combinedMultiplier = combinedMultiplier.multiply(modifier.multiplier());
            
            log.debug("Applied {}: {} ({})", 
                strategy.getStrategyName(), 
                modifier.multiplier(), 
                modifier.description());
        }

        BigDecimal baseTotalPrice = context.basePrice()
            .multiply(BigDecimal.valueOf(context.rentalDays()));

        BigDecimal calculatedPrice = baseTotalPrice.multiply(combinedMultiplier)
            .setScale(places, RoundingMode.HALF_UP);

        BigDecimal finalPrice = applyPriceCaps(calculatedPrice, context.rentalDays(), currency);

        log.info("Price calculation complete: base={}, combined={}, final={}", 
            baseTotalPrice, calculatedPrice, finalPrice);

        return PricingResult.builder()
            .basePrice(context.basePrice())
            .rentalDays(context.rentalDays())
            .appliedModifiers(appliedModifiers)
            .combinedMultiplier(combinedMultiplier)
            .finalPrice(finalPrice)
            .scale(places)
            .build();
    }

    private BigDecimal applyPriceCaps(BigDecimal calculatedPrice, int rentalDays, CurrencyType currency) {
        if (rentalDays <= 0) {
            throw new IllegalArgumentException("Rental days must be positive");
        }
        int places = currency.getDecimalPlaces();
        BigDecimal dailyPrice = calculatedPrice.divide(
            BigDecimal.valueOf(rentalDays), 
            places, 
            RoundingMode.HALF_UP
        );

        BigDecimal minDailyPrice = config.minDailyPriceFor(currency);
        BigDecimal maxDailyPrice = config.maxDailyPriceFor(currency);

        if (dailyPrice.compareTo(minDailyPrice) < 0) {
            log.warn("Daily price {} below minimum {}, applying cap", dailyPrice, minDailyPrice);
            return minDailyPrice.multiply(BigDecimal.valueOf(rentalDays)).setScale(places, RoundingMode.HALF_UP);
        }

        if (dailyPrice.compareTo(maxDailyPrice) > 0) {
            log.warn("Daily price {} above maximum {}, applying cap", dailyPrice, maxDailyPrice);
            return maxDailyPrice.multiply(BigDecimal.valueOf(rentalDays)).setScale(places, RoundingMode.HALF_UP);
        }

        return calculatedPrice;
    }

    private PricingContext createContext(Car car, LocalDate startDate, LocalDate endDate, LocalDate bookingDate,
                                         Long excludedBookingId) {
        int rentalDays = RentalDays.inclusiveDays(startDate, endDate);
        int leadTimeDays = RentalDays.daysBetween(bookingDate, startDate);

        return new PricingContext(
            car.getId(),
            car.getPrice(),
            startDate,
            endDate,
            bookingDate,
            rentalDays,
            leadTimeDays,
            car.getBodyType() != null ? car.getBodyType() : "UNKNOWN",
            excludedBookingId,
            null
        );
    }
}
