package com.akif.car.internal.service.pricing.strategy;

import com.akif.shared.time.RentalDays;
import com.akif.car.internal.config.PricingConfig;
import com.akif.car.api.PriceModifier;
import com.akif.car.api.PricingStrategyCode;
import com.akif.car.internal.dto.pricing.PricingContext;
import com.akif.car.internal.service.pricing.PricingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class WeekendPricingStrategy implements PricingStrategy {

    private final PricingConfig config;

    @Override
    public PriceModifier calculate(PricingContext context) {
        int weekendDays = countWeekendDays(context.startDate(), context.endDate());
        int totalDays = context.rentalDays();

        if (weekendDays == 0) {
            return PriceModifier.neutral(getCode(), getStrategyName(), "No weekend days");
        }

        BigDecimal weekendMultiplier = config.weekend().multiplier();
        BigDecimal weekdayMultiplier = BigDecimal.ONE;

        BigDecimal weekendPortion = weekendMultiplier.multiply(BigDecimal.valueOf(weekendDays));
        BigDecimal weekdayPortion = weekdayMultiplier.multiply(BigDecimal.valueOf(totalDays - weekendDays));
        BigDecimal weightedMultiplier = weekendPortion.add(weekdayPortion)
            .divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP);

        String description = String.format("Weekend pricing (%d/%d days)", weekendDays, totalDays);

        if (weightedMultiplier.compareTo(BigDecimal.ONE) > 0) {
            return PriceModifier.surcharge(getCode(), getStrategyName(), weightedMultiplier, description);
        }

        return PriceModifier.neutral(getCode(), getStrategyName(), description);
    }

    private int countWeekendDays(LocalDate startDate, LocalDate endDate) {
        return (int) RentalDays.eachDay(startDate, endDate)
                .filter(date -> config.weekend().days().contains(date.getDayOfWeek()))
                .count();
    }

    @Override
    public PricingStrategyCode getCode() {
        return PricingStrategyCode.WEEKEND;
    }

    @Override
    public String getStrategyName() {
        return "Weekend Pricing";
    }

    @Override
    public boolean isEnabled() {
        return config.strategy().weekendEnabled();
    }

    @Override
    public int getOrder() {
        return 4;
    }
}
