package com.akif.currency.api;

import com.akif.shared.enums.CurrencyType;
import com.akif.currency.api.RateSource;

import java.math.BigDecimal;
import java.time.Instant;

public record ExchangeRate(
        CurrencyType from,
        CurrencyType to,
        BigDecimal rate,
        Instant timestamp,
        RateSource source
) {
    
    public boolean isLive() {
        return source == RateSource.LIVE;
    }
    
    public boolean isFallback() {
        return source == RateSource.FALLBACK;
    }
    
    public boolean isSameCurrency() {
        return from.equals(to);
    }
}
