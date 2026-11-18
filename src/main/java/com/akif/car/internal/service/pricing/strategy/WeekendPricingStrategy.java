package com.akif.car.internal.service.pricing.strategy;

import com.akif.car.internal.config.PricingConfig;
import com.akif.car.api.PriceModifier;
import com.akif.car.internal.dto.pricing.PricingContext;
import com.akif.car.internal.service.pricing.PricingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
@Order(4)
@RequiredArgsConstructor
public class WeekendPricingStrategy implements PricingStrategy {

    private final PricingConfig config;

    @Override
    public PriceModifier calculate(PricingContext context) {
        int weekendDays = countWeekendDays(context.startDate(), context.endDate());
        int totalDays = context.rentalDays();

        if (weekendDays == 0) {
            return PriceModifier.neutral(getStrategyName(), "No weekend days");
        }

        BigDecimal weekendMultiplier = config.getWeekend().getMultiplier();
        BigDecimal weekdayMultiplier = BigDecimal.ONE;

        BigDecimal weekendPortion = weekendMultiplier.multiply(BigDecimal.valueOf(weekendDays));
        BigDecimal weekdayPortion = weekdayMultiplier.multiply(BigDecimal.valueOf(totalDays - weekendDays));
        BigDecimal weightedMultiplier = weekendPortion.add(weekdayPortion)
            .divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP);

        String description = String.format("Weekend pricing (%d/%d days)", weekendDays, totalDays);

        if (weightedMultiplier.compareTo(BigDecimal.ONE) > 0) {
            return PriceModifier.surcharge(getStrategyName(), weightedMultiplier, description);
        }

        return PriceModifier.neutral(getStrategyName(), description);
    }

    private int countWeekendDays(LocalDate startDate, LocalDate endDate) {
        int count = 0;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (config.getWeekend().getDays().contains(dayOfWeek)) {
                count++;
            }
            current = current.plusDays(1);
        }

        return count;
    }

    @Override
    public String getStrategyName() {
        return "Weekend Pricing";
    }

    @Override
    public boolean isEnabled() {
        return config.getStrategy().isWeekendEnabled();
    }

    @Override
    public int getOrder() {
        return 4;
    }
}
