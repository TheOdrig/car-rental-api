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
import java.time.LocalDate;
import java.time.MonthDay;

@Component
@RequiredArgsConstructor
public class SeasonPricingStrategy implements PricingStrategy {

    private final PricingConfig config;

    @Override
    public PriceModifier calculate(PricingContext context) {
        int peakDays = countDaysInSeason(
            context.startDate(), 
            context.endDate(),
            config.season().peak().start(),
            config.season().peak().end()
        );

        int offpeakDays = countDaysInSeason(
            context.startDate(),
            context.endDate(),
            config.season().offpeak().start(),
            config.season().offpeak().end()
        );

        int totalDays = context.rentalDays();
        int regularDays = totalDays - peakDays - offpeakDays;

        if (peakDays == 0 && offpeakDays == 0) {
            return PriceModifier.neutral(getCode(), getStrategyName(), "Regular season");
        }

        BigDecimal peakMultiplier = config.season().peak().multiplier();
        BigDecimal offpeakMultiplier = config.season().offpeak().multiplier();
        BigDecimal regularMultiplier = BigDecimal.ONE;

        BigDecimal peakPortion = peakMultiplier.multiply(BigDecimal.valueOf(peakDays));
        BigDecimal offpeakPortion = offpeakMultiplier.multiply(BigDecimal.valueOf(offpeakDays));
        BigDecimal regularPortion = regularMultiplier.multiply(BigDecimal.valueOf(regularDays));

        BigDecimal weightedMultiplier = peakPortion.add(offpeakPortion).add(regularPortion)
            .divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP);

        String description = buildDescription(peakDays, offpeakDays, regularDays, totalDays);

        if (weightedMultiplier.compareTo(BigDecimal.ONE) > 0) {
            return PriceModifier.surcharge(getCode(), getStrategyName(), weightedMultiplier, description);
        } else if (weightedMultiplier.compareTo(BigDecimal.ONE) < 0) {
            return PriceModifier.discount(getCode(), getStrategyName(), weightedMultiplier, description);
        }

        return PriceModifier.neutral(getCode(), getStrategyName(), description);
    }

    private int countDaysInSeason(LocalDate startDate, LocalDate endDate, MonthDay seasonStart, MonthDay seasonEnd) {
        return (int) RentalDays.eachDay(startDate, endDate)
                .filter(date -> isDateInSeason(date, seasonStart, seasonEnd))
                .count();
    }

    private boolean isDateInSeason(LocalDate date, MonthDay seasonStart, MonthDay seasonEnd) {
        MonthDay dateMonthDay = MonthDay.from(date);

        if (seasonStart.isAfter(seasonEnd)) {
            return dateMonthDay.compareTo(seasonStart) >= 0 || dateMonthDay.compareTo(seasonEnd) <= 0;
        }

        return dateMonthDay.compareTo(seasonStart) >= 0 && dateMonthDay.compareTo(seasonEnd) <= 0;
    }

    private String buildDescription(int peakDays, int offpeakDays, int regularDays, int totalDays) {
        if (peakDays > 0 && offpeakDays > 0) {
            return String.format("Mixed season (%d peak, %d off-peak, %d regular of %d days)",
                peakDays, offpeakDays, regularDays, totalDays);
        } else if (peakDays > 0) {
            return String.format("Peak season (%d/%d days)", peakDays, totalDays);
        } else if (offpeakDays > 0) {
            return String.format("Off-peak season (%d/%d days)", offpeakDays, totalDays);
        }
        return "Regular season";
    }

    @Override
    public PricingStrategyCode getCode() {
        return PricingStrategyCode.SEASON;
    }

    @Override
    public String getStrategyName() {
        return "Season Pricing";
    }

    @Override
    public boolean isEnabled() {
        return config.strategy().seasonEnabled();
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
