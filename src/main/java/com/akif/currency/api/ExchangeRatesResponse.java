package com.akif.currency.api;

import com.akif.shared.enums.CurrencyType;
import com.akif.currency.api.RateSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record ExchangeRatesResponse(
        CurrencyType baseCurrency,
        Map<CurrencyType, BigDecimal> rates,
        Instant timestamp,
        RateSource source
) {
    
    public int getRateCount() {
        return rates != null ? rates.size() : 0;
    }
    
    public boolean hasRate(CurrencyType currency) {
        return rates != null && rates.containsKey(currency);
    }
    
    public BigDecimal getRate(CurrencyType currency) {
        return rates != null ? rates.get(currency) : null;
    }
}
