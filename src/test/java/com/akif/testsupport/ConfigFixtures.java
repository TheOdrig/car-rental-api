package com.akif.testsupport;

import com.akif.car.internal.config.PricingConfig;
import com.akif.rental.internal.config.PenaltyConfig;
import com.akif.rental.internal.config.RentalProperties;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.MonthDay;
import java.util.List;
import java.util.Map;

public final class ConfigFixtures {

    private ConfigFixtures() {
    }

    public static RentalProperties rentalProperties() {
        return new RentalProperties(30, 180, 3, 24, 5, 12, 1);
    }

    public static RentalProperties rentalPropertiesWithRequestExpiryHours(int hours) {
        return new RentalProperties(30, 180, 3, hours, 5, 12, 1);
    }

    public static PenaltyConfig penaltyConfig() {
        return new PenaltyConfig(60, new BigDecimal("0.10"), new BigDecimal("1.50"), new BigDecimal("5.0"), 24);
    }

    public static PenaltyConfig penaltyConfig(int graceMinutes, String hourlyRate, String dailyRate,
                                              int severelyLateHours) {
        return new PenaltyConfig(graceMinutes, new BigDecimal(hourlyRate), new BigDecimal(dailyRate),
                new BigDecimal("5.0"), severelyLateHours);
    }

    public static PricingConfig pricingConfig() {
        return new PricingConfig(new BigDecimal("100"), new BigDecimal("10000"), Map.of(),
                season(), earlyBooking(), duration(), weekend(), demand(), strategies(true));
    }

    public static PricingConfig.SeasonConfig season() {
        return new PricingConfig.SeasonConfig(
                new PricingConfig.SeasonPeriod(MonthDay.of(6, 1), MonthDay.of(8, 31), new BigDecimal("1.25")),
                new PricingConfig.SeasonPeriod(MonthDay.of(11, 1), MonthDay.of(2, 28), new BigDecimal("0.90")));
    }

    public static PricingConfig.EarlyBookingConfig earlyBooking() {
        return new PricingConfig.EarlyBookingConfig(
                new PricingConfig.Tier(30, new BigDecimal("0.85")),
                new PricingConfig.Tier(14, new BigDecimal("0.90")),
                new PricingConfig.Tier(7, new BigDecimal("0.95")));
    }

    public static PricingConfig.DurationConfig duration() {
        return new PricingConfig.DurationConfig(
                new PricingConfig.Tier(7, new BigDecimal("0.90")),
                new PricingConfig.Tier(14, new BigDecimal("0.85")),
                new PricingConfig.Tier(30, new BigDecimal("0.80")));
    }

    public static PricingConfig.WeekendConfig weekend() {
        return new PricingConfig.WeekendConfig(new BigDecimal("1.15"),
                List.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
    }

    public static PricingConfig.DemandConfig demand() {
        return new PricingConfig.DemandConfig(
                new PricingConfig.DemandTier(80, new BigDecimal("1.20")),
                new PricingConfig.DemandTier(50, new BigDecimal("1.10")));
    }

    public static PricingConfig.StrategyConfig strategies(boolean enabled) {
        return new PricingConfig.StrategyConfig(enabled, enabled, enabled, enabled, enabled);
    }
}
