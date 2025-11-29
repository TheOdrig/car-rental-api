package com.akif.service.pricing.impl;

import com.akif.config.PricingConfig;
import com.akif.exception.CarNotFoundException;
import com.akif.model.Car;
import com.akif.repository.CarRepository;
import com.akif.service.pricing.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicPricingServiceImpl implements IDynamicPricingService {

    private final List<PricingStrategy> allStrategies;
    private final CarRepository carRepository;
    private final PricingConfig config;

    @Override
    public PricingResult calculatePrice(Long carId, LocalDate startDate, LocalDate endDate, LocalDate bookingDate) {
        log.debug("Calculating price for car: {}, dates: {} to {}, booking: {}", 
            carId, startDate, endDate, bookingDate);

        Car car = carRepository.findByIdAndIsDeletedFalse(carId)
            .orElseThrow(() -> new CarNotFoundException(carId));

        PricingContext context = createContext(car, startDate, endDate, bookingDate);
        
        return calculatePriceWithContext(context);
    }

    @Override
    public PricingResult previewPrice(Long carId, LocalDate startDate, LocalDate endDate) {
        return calculatePrice(carId, startDate, endDate, LocalDate.now());
    }

    @Override
    public List<PricingStrategy> getEnabledStrategies() {
        return allStrategies.stream()
            .filter(PricingStrategy::isEnabled)
            .sorted(Comparator.comparingInt(PricingStrategy::getOrder))
            .toList();
    }

    private PricingResult calculatePriceWithContext(PricingContext context) {
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
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal finalPrice = applyPriceCaps(calculatedPrice, context.rentalDays());

        log.info("Price calculation complete: base={}, combined={}, final={}", 
            baseTotalPrice, calculatedPrice, finalPrice);

        return PricingResult.builder()
            .basePrice(context.basePrice())
            .rentalDays(context.rentalDays())
            .appliedModifiers(appliedModifiers)
            .combinedMultiplier(combinedMultiplier)
            .finalPrice(finalPrice)
            .build();
    }

    private BigDecimal applyPriceCaps(BigDecimal calculatedPrice, int rentalDays) {
        BigDecimal dailyPrice = calculatedPrice.divide(
            BigDecimal.valueOf(rentalDays), 
            2, 
            RoundingMode.HALF_UP
        );

        BigDecimal minDailyPrice = config.getMinDailyPrice();
        BigDecimal maxDailyPrice = config.getMaxDailyPrice();

        if (dailyPrice.compareTo(minDailyPrice) < 0) {
            log.warn("Daily price {} below minimum {}, applying cap", dailyPrice, minDailyPrice);
            return minDailyPrice.multiply(BigDecimal.valueOf(rentalDays));
        }

        if (dailyPrice.compareTo(maxDailyPrice) > 0) {
            log.warn("Daily price {} above maximum {}, applying cap", dailyPrice, maxDailyPrice);
            return maxDailyPrice.multiply(BigDecimal.valueOf(rentalDays));
        }

        return calculatedPrice;
    }

    private PricingContext createContext(Car car, LocalDate startDate, LocalDate endDate, LocalDate bookingDate) {
        int rentalDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        int leadTimeDays = (int) ChronoUnit.DAYS.between(bookingDate, startDate);

        return new PricingContext(
            car.getId(),
            car.getPrice(),
            startDate,
            endDate,
            bookingDate,
            rentalDays,
            leadTimeDays,
            car.getBodyType() != null ? car.getBodyType() : "UNKNOWN"
        );
    }
}
