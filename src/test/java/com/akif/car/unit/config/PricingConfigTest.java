package com.akif.car.unit.config;

import com.akif.car.internal.config.PricingConfig;
import com.akif.shared.enums.CurrencyType;
import com.akif.testsupport.ConfigFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PricingConfig daily limits")
class PricingConfigTest {

    private PricingConfig withLimits(Map<CurrencyType, PricingConfig.DailyLimits> limits) {
        return new PricingConfig(new BigDecimal("100"), new BigDecimal("10000"), limits,
                ConfigFixtures.season(), ConfigFixtures.earlyBooking(), ConfigFixtures.duration(),
                ConfigFixtures.weekend(), ConfigFixtures.demand(), ConfigFixtures.strategies(true));
    }

    @Test
    @DisplayName("falls back to the default limits when a currency has no entry")
    void fallsBackToDefaults() {
        PricingConfig config = withLimits(Map.of());

        assertThat(config.minDailyPriceFor(CurrencyType.USD)).isEqualByComparingTo("100");
        assertThat(config.maxDailyPriceFor(CurrencyType.USD)).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("uses the per-currency limits when configured and falls back for a missing bound")
    void usesPerCurrencyLimits() {
        PricingConfig config = withLimits(Map.of(CurrencyType.JPY,
                new PricingConfig.DailyLimits(new BigDecimal("1000"), null)));

        assertThat(config.minDailyPriceFor(CurrencyType.JPY)).isEqualByComparingTo("1000");
        assertThat(config.maxDailyPriceFor(CurrencyType.JPY)).isEqualByComparingTo("10000");
        assertThat(config.minDailyPriceFor(CurrencyType.EUR)).isEqualByComparingTo("100");
    }
}
