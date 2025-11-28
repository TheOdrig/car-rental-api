package com.akif.dto.currency;

import com.akif.enums.CurrencyType;
import com.akif.enums.RateSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConversionResult(
        BigDecimal originalAmount,
        CurrencyType originalCurrency,
        BigDecimal convertedAmount,
        CurrencyType targetCurrency,
        BigDecimal exchangeRate,
        LocalDateTime rateTimestamp,
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
