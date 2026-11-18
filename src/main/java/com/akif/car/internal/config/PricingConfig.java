package com.akif.rental.internal.config;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.MonthDay;
import java.util.List;

@Getter
@Setter
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "pricing")
public class PricingConfig {

    private BigDecimal minDailyPrice = new BigDecimal("100");
    private BigDecimal maxDailyPrice = new BigDecimal("10000");
    private SeasonConfig season = new SeasonConfig();
    private EarlyBookingConfig earlyBooking = new EarlyBookingConfig();
    private DurationConfig duration = new DurationConfig();
    private WeekendConfig weekend = new WeekendConfig();
    private DemandConfig demand = new DemandConfig();
    private StrategyConfig strategy = new StrategyConfig();

    @PostConstruct
    public void validate() {
        validateMultiplier("season.peak.multiplier", season.getPeak().getMultiplier(), false);
        validateMultiplier("season.offpeak.multiplier", season.getOffpeak().getMultiplier(), true);
        validateMultiplier("earlyBooking.tier1.multiplier", earlyBooking.getTier1().getMultiplier(), true);
        validateMultiplier("earlyBooking.tier2.multiplier", earlyBooking.getTier2().getMultiplier(), true);
        validateMultiplier("earlyBooking.tier3.multiplier", earlyBooking.getTier3().getMultiplier(), true);
        validateMultiplier("duration.tier1.multiplier", duration.getTier1().getMultiplier(), true);
        validateMultiplier("duration.tier2.multiplier", duration.getTier2().getMultiplier(), true);
        validateMultiplier("duration.tier3.multiplier", duration.getTier3().getMultiplier(), true);
        validateMultiplier("weekend.multiplier", weekend.getMultiplier(), false);
        validateMultiplier("demand.high.multiplier", demand.getHigh().getMultiplier(), false);
        validateMultiplier("demand.moderate.multiplier", demand.getModerate().getMultiplier(), false);
    }

    private void validateMultiplier(String name, BigDecimal value, boolean isDiscount) {
        if (isDiscount) {

            if (value.compareTo(new BigDecimal("0.50")) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
                log.warn("Invalid discount multiplier for {}: {}. Using default.", name, value);
            }
        } else {

            if (value.compareTo(BigDecimal.ONE) < 0 || value.compareTo(new BigDecimal("2.00")) > 0) {
                log.warn("Invalid surcharge multiplier for {}: {}. Using default.", name, value);
            }
        }
    }


    @Getter
    @Setter
    public static class SeasonConfig {
        private SeasonPeriod peak = new SeasonPeriod(MonthDay.of(6, 1), MonthDay.of(8, 31), new BigDecimal("1.25"));
        private SeasonPeriod offpeak = new SeasonPeriod(MonthDay.of(11, 1), MonthDay.of(2, 28), new BigDecimal("0.90"));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeasonPeriod {
        private MonthDay start;
        private MonthDay end;
        private BigDecimal multiplier;
    }

    @Getter
    @Setter
    public static class EarlyBookingConfig {
        private Tier tier1 = new Tier(30, new BigDecimal("0.85"));
        private Tier tier2 = new Tier(14, new BigDecimal("0.90"));
        private Tier tier3 = new Tier(7, new BigDecimal("0.95"));
    }

    @Getter
    @Setter
    public static class DurationConfig {
        private Tier tier1 = new Tier(7, new BigDecimal("0.90"));
        private Tier tier2 = new Tier(14, new BigDecimal("0.85"));
        private Tier tier3 = new Tier(30, new BigDecimal("0.80"));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tier {
        private int days;
        private BigDecimal multiplier;
    }

    @Getter
    @Setter
    public static class WeekendConfig {
        private BigDecimal multiplier = new BigDecimal("1.15");
        private List<DayOfWeek> days = List.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    }

    @Getter
    @Setter
    public static class DemandConfig {
        private DemandTier high = new DemandTier(80, new BigDecimal("1.20"));
        private DemandTier moderate = new DemandTier(50, new BigDecimal("1.10"));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DemandTier {
        private int threshold;
        private BigDecimal multiplier;
    }

    @Getter
    @Setter
    public static class StrategyConfig {
        private boolean seasonEnabled = true;
        private boolean earlyBookingEnabled = true;
        private boolean durationEnabled = true;
        private boolean weekendEnabled = true;
        private boolean demandEnabled = true;
    }
}
