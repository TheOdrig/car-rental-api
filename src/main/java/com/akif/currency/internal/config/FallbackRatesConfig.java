package com.akif.currency.internal.config;

import com.akif.shared.enums.CurrencyType;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "currency")
public record FallbackRatesConfig(Map<CurrencyType, BigDecimal> fallback) {

    public FallbackRatesConfig {
        fallback = fallback == null ? Map.of() : Map.copyOf(fallback);
    }

    public Map<CurrencyType, BigDecimal> rates() {
        return fallback;
    }

    @AssertTrue(message = "currency.fallback must define a positive rate for every supported currency")
    public boolean isComplete() {
        for (CurrencyType currency : CurrencyType.values()) {
            BigDecimal rate = fallback.get(currency);
            if (rate == null || rate.signum() <= 0) {
                return false;
            }
        }
        return true;
    }
}
