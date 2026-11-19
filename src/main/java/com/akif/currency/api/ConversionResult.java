package com.akif.currency.api;

import com.akif.shared.enums.CurrencyType;
import com.akif.currency.api.RateSource;

import java.math.BigDecimal;
import java.time.Instant;

public record ConversionResult(
        BigDecimal originalAmount,
        CurrencyType originalCurrency,
        BigDecimal convertedAmount,
        CurrencyType targetCurrency,
        BigDecimal exchangeRate,
        Instant rateTimestamp,
        RateSource source
) {
    
    public boolean isConverted() {
        return !originalCurrency.equals(targetCurrency);
    }
    
    public boolean isFallback() {
        return source == RateSource.FALLBACK;
    }
    
    public String getFormattedOriginal() {
        return originalCurrency.formatAmount(originalAmount);
    }
    
    public String getFormattedConverted() {
        return targetCurrency.formatAmount(convertedAmount);
    }
}
