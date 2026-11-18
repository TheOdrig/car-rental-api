package com.akif.car.unit.config;

import com.akif.car.internal.config.PricingConfig;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PricingConfig daily limits")
class PricingConfigTest {

    @Test
    @DisplayName("falls back to the default limits when a currency has no entry")
    void fallsBackToDefaults() {
        PricingConfig config = new PricingConfig();

        assertThat(config.minDailyPriceFor(CurrencyType.USD)).isEqualByComparingTo("100");
        assertThat(config.maxDailyPriceFor(CurrencyType.USD)).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("uses the per-currency limits when configured and falls back for a missing bound")
    void usesPerCurrencyLimits() {
        PricingConfig config = new PricingConfig();
        PricingConfig.DailyLimits yen = new PricingConfig.DailyLimits();
        yen.setMin(new BigDecimal("1000"));
        config.getDailyLimits().put(CurrencyType.JPY, yen);

        assertThat(config.minDailyPriceFor(CurrencyType.JPY)).isEqualByComparingTo("1000");
        assertThat(config.maxDailyPriceFor(CurrencyType.JPY)).isEqualByComparingTo("10000");
        assertThat(config.minDailyPriceFor(CurrencyType.EUR)).isEqualByComparingTo("100");
    }
}
