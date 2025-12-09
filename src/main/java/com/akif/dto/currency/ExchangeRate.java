package com.akif.dto.currency;

import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.RateSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExchangeRate(
        CurrencyType from,
        CurrencyType to,
        BigDecimal rate,
        LocalDateTime timestamp,
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
