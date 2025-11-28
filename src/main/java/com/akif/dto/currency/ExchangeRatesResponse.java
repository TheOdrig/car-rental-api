package com.akif.dto.currency;

import com.akif.enums.CurrencyType;
import com.akif.enums.RateSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record ExchangeRatesResponse(
        CurrencyType baseCurrency,
        Map<CurrencyType, BigDecimal> rates,
        LocalDateTime timestamp,
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
