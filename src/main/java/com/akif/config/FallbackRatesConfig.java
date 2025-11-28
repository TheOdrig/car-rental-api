package com.akif.config;

import com.akif.enums.CurrencyType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "currency.fallback")
@Getter
@Setter
public class FallbackRatesConfig {

    private BigDecimal usd = BigDecimal.ONE;
    private BigDecimal tryRate = new BigDecimal("42.49");
    private BigDecimal eur = new BigDecimal("0.87");
    private BigDecimal gbp = new BigDecimal("0.76");
    private BigDecimal jpy = new BigDecimal("156.00");

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
