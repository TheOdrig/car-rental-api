package com.akif.currency.internal.config;

import com.akif.shared.enums.CurrencyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "currency.fallback")
public record FallbackRatesConfig(
        @DefaultValue("1") @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal usd,
        @DefaultValue("42.49") @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal tryRate,
        @DefaultValue("0.87") @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal eur,
        @DefaultValue("0.76") @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal gbp,
        @DefaultValue("156.00") @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal jpy) {

    public Map<CurrencyType, BigDecimal> getRates() {
        Map<CurrencyType, BigDecimal> rates = new EnumMap<>(CurrencyType.class);
        rates.put(CurrencyType.USD, usd);
        rates.put(CurrencyType.TRY, tryRate);
        rates.put(CurrencyType.EUR, eur);
        rates.put(CurrencyType.GBP, gbp);
        rates.put(CurrencyType.JPY, jpy);
        return rates;
    }
}
