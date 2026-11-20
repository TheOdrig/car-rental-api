package com.akif.car.internal.config;

import com.akif.shared.enums.CurrencyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.MonthDay;
import java.util.List;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "pricing")
public record PricingConfig(
        @DefaultValue("100") @NotNull @DecimalMin("0.01") BigDecimal minDailyPrice,
        @DefaultValue("10000") @NotNull @DecimalMin("0.01") BigDecimal maxDailyPrice,
        Map<CurrencyType, @Valid @NotNull DailyLimits> dailyLimits,
        @Valid @NotNull SeasonConfig season,
        @Valid @NotNull EarlyBookingConfig earlyBooking,
        @Valid @NotNull DurationConfig duration,
        @Valid @NotNull WeekendConfig weekend,
        @Valid @NotNull DemandConfig demand,
        @DefaultValue @Valid @NotNull StrategyConfig strategy) {

    private static final String DISCOUNT_MIN = "0.50";
    private static final String DISCOUNT_MAX = "1.00";
    private static final String SURCHARGE_MIN = "1.00";
    private static final String SURCHARGE_MAX = "2.00";

    public PricingConfig {
        dailyLimits = dailyLimits == null ? Map.of() : Map.copyOf(dailyLimits);
    }

    public BigDecimal minDailyPriceFor(CurrencyType currency) {
        DailyLimits limits = dailyLimits.get(currency);
        return limits != null && limits.min() != null ? limits.min() : minDailyPrice;
    }

    public BigDecimal maxDailyPriceFor(CurrencyType currency) {
        DailyLimits limits = dailyLimits.get(currency);
        return limits != null && limits.max() != null ? limits.max() : maxDailyPrice;
    }

    @AssertTrue(message = "pricing.min-daily-price must not exceed pricing.max-daily-price")
    public boolean isDefaultLimitsOrdered() {
        return minDailyPrice == null || maxDailyPrice == null || minDailyPrice.compareTo(maxDailyPrice) <= 0;
    }

    @AssertTrue(message = "every pricing.daily-limits entry must have min not above max")
    public boolean isCurrencyLimitsOrdered() {
        return dailyLimits.keySet().stream().allMatch(currency -> {
            BigDecimal low = minDailyPriceFor(currency);
            BigDecimal high = maxDailyPriceFor(currency);
            return low == null || high == null || low.compareTo(high) <= 0;
        });
    }

    public record DailyLimits(
            @DecimalMin("0.01") BigDecimal min,
            @DecimalMin("0.01") BigDecimal max) {
    }

    public record SeasonConfig(
            @Valid @NotNull SeasonPeriod peak,
            @Valid @NotNull SeasonPeriod offpeak) {

        @AssertTrue(message = "pricing.season.peak.multiplier must be a surcharge and pricing.season.offpeak.multiplier a discount")
        public boolean isMultipliersDirected() {
            return peak == null || offpeak == null || peak.multiplier() == null || offpeak.multiplier() == null
                    || (peak.multiplier().compareTo(BigDecimal.ONE) >= 0
                            && offpeak.multiplier().compareTo(BigDecimal.ONE) <= 0);
        }
    }

    public record SeasonPeriod(
            @NotNull MonthDay start,
            @NotNull MonthDay end,
            @NotNull @DecimalMin(DISCOUNT_MIN) @DecimalMax(SURCHARGE_MAX) BigDecimal multiplier) {
    }

    public record EarlyBookingConfig(
            @Valid @NotNull Tier tier1,
            @Valid @NotNull Tier tier2,
            @Valid @NotNull Tier tier3) {

        @AssertTrue(message = "pricing.early-booking tiers must have strictly decreasing days")
        public boolean isTiersOrdered() {
            return tier1 == null || tier2 == null || tier3 == null
                    || (tier1.days() > tier2.days() && tier2.days() > tier3.days());
        }
    }

    public record DurationConfig(
            @Valid @NotNull Tier tier1,
            @Valid @NotNull Tier tier2,
            @Valid @NotNull Tier tier3) {

        @AssertTrue(message = "pricing.duration tiers must have strictly increasing days")
        public boolean isTiersOrdered() {
            return tier1 == null || tier2 == null || tier3 == null
                    || (tier1.days() < tier2.days() && tier2.days() < tier3.days());
        }
    }

    public record Tier(
            @Min(1) int days,
            @NotNull @DecimalMin(DISCOUNT_MIN) @DecimalMax(DISCOUNT_MAX) BigDecimal multiplier) {
    }

    public record WeekendConfig(
            @NotNull @DecimalMin(SURCHARGE_MIN) @DecimalMax(SURCHARGE_MAX) BigDecimal multiplier,
            @NotEmpty List<DayOfWeek> days) {
    }

    public record DemandConfig(
            @Valid @NotNull DemandTier high,
            @Valid @NotNull DemandTier moderate) {

        @AssertTrue(message = "pricing.demand.high.threshold must exceed pricing.demand.moderate.threshold")
        public boolean isThresholdsOrdered() {
            return high == null || moderate == null || high.threshold() > moderate.threshold();
        }
    }

    public record DemandTier(
            @Min(0) @Max(100) int threshold,
            @NotNull @DecimalMin(SURCHARGE_MIN) @DecimalMax(SURCHARGE_MAX) BigDecimal multiplier) {
    }

    public record StrategyConfig(
            @DefaultValue("true") boolean seasonEnabled,
            @DefaultValue("true") boolean earlyBookingEnabled,
            @DefaultValue("true") boolean durationEnabled,
            @DefaultValue("true") boolean weekendEnabled,
            @DefaultValue("true") boolean demandEnabled) {
    }
}
