package com.akif.currency.internal.config;

import com.akif.shared.enums.CurrencyType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "currency")
public record FallbackRatesConfig(Map<CurrencyType, BigDecimal> fallback) {

    public FallbackRatesConfig {
        fallback = fallback == null ? Map.of() : Map.copyOf(fallback);
        for (CurrencyType currency : CurrencyType.values()) {
            BigDecimal rate = fallback.get(currency);
            if (rate == null || rate.signum() <= 0) {
                throw new IllegalArgumentException("currency.fallback must define a positive rate for " + currency);
            }
        }
    }

    public Map<CurrencyType, BigDecimal> rates() {
        return fallback;
    }
}
