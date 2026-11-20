package com.akif.shared.config;

import com.akif.car.internal.config.PricingConfig;
import com.akif.car.internal.config.SimilarCarProperties;
import com.akif.currency.internal.config.FallbackRatesConfig;
import com.akif.damage.internal.config.DamageConfig;
import com.akif.rental.internal.config.PenaltyConfig;
import com.akif.rental.internal.config.RentalProperties;
import com.akif.shared.security.ratelimit.RateLimitProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Validated configuration fails startup on invalid values")
class ConfigurationFailFastTest {

    @Configuration
    @EnableConfigurationProperties({PricingConfig.class, PenaltyConfig.class, RentalProperties.class,
            DamageConfig.class, FallbackRatesConfig.class, RateLimitProperties.class, SimilarCarProperties.class,
            com.akif.dashboard.internal.config.AlertProperties.class,
            com.akif.auth.internal.config.AvatarProperties.class,
            com.akif.auth.internal.config.RefreshTokenProperties.class,
            com.akif.rental.internal.config.ReminderProperties.class,
            com.akif.car.internal.config.AvailabilityProperties.class})
    static class Config {
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(Config.class);
    }

    @Test
    @DisplayName("starts with the shipped application.properties")
    void startsWithShippedConfiguration() {
        runner().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(PricingConfig.class).season().peak().multiplier()).isEqualByComparingTo("1.25");
            assertThat(context.getBean(PenaltyConfig.class).gracePeriodMinutes()).isEqualTo(60);
        });
    }

    @Test
    @DisplayName("rejects a penalty grace period out of range instead of clamping it")
    void rejectsInvalidPenalty() {
        runner().withPropertyValues("penalty.grace-period-minutes=500")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("rejects a pricing multiplier out of range instead of logging and keeping it")
    void rejectsInvalidPricingMultiplier() {
        runner().withPropertyValues("pricing.season.peak.multiplier=3.5")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("rejects early booking tiers that are not strictly decreasing")
    void rejectsUnorderedTiers() {
        runner().withPropertyValues("pricing.early-booking.tier2.days=40")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("rejects an hourly penalty tier that reaches the severely late threshold")
    void rejectsHourlyTierAboveSeverelyLate() {
        runner().withPropertyValues("penalty.hourly-tier-max-hours=30")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("rejects a payment grace period out of range")
    void rejectsInvalidRentalGrace() {
        runner().withPropertyValues("rental.payment-grace-hours=500")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("rejects a no-show window that would outlive the authorisation hold")
    void rejectsInvalidNoShow() {
        runner().withPropertyValues("rental.no-show-days=4")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("rejects damage thresholds that are not increasing")
    void rejectsUnorderedDamageThresholds() {
        runner().withPropertyValues("damage.minor-threshold=5000")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("rejects a non-positive rate limit capacity")
    void rejectsInvalidRateLimit() {
        runner().withPropertyValues("security.rate-limit.limits.login.capacity=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("rejects similar-car weights above 100")
    void rejectsInvalidSimilarWeights() {
        runner().withPropertyValues("car.similar.brand-weight=500")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("rejects damage currency thresholds that are not increasing")
    void rejectsUnorderedCurrencyDamageThresholds() {
        runner().withPropertyValues("damage.currency-limits.jpy.minor-threshold=50000")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("rejects an alert availability threshold above 100 percent")
    void rejectsInvalidAlertThreshold() {
        runner().withPropertyValues("dashboard.alerts.low-availability-percent=150")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("rejects a zero reminder batch size")
    void rejectsInvalidReminderBatch() {
        runner().withPropertyValues("rental.reminder.batch-size=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("rejects a non-positive fallback exchange rate")
    void rejectsInvalidFallbackRate() {
        runner().withPropertyValues("currency.fallback.eur=0")
                .run(context -> assertThat(context).hasFailed());
    }
}
