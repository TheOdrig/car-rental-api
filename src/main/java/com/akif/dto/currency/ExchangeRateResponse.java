package com.akif.dto.currency;

import com.akif.enums.CurrencyType;
import com.akif.enums.RateSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record ExchangeRateResponse(
        CurrencyType baseCurrency,
        LocalDateTime timestamp,
        Map<CurrencyType, BigDecimal> rates,
        RateSource source
) {
    
    public boolean hasRate(CurrencyType currency) {
        return rates != null && rates.containsKey(currency);
    }
    
    public BigDecimal getRate(CurrencyType currency) {
        if (rates == null) {
            return null;
        }
        return rates.get(currency);
    }
    
    public boolean isLive() {
        return source == RateSource.LIVE;
    }
    
    public boolean isFallback() {
        return source == RateSource.FALLBACK;
    }
}
