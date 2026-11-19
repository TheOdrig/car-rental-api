package com.akif.currency.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;

public record DisplayPrice(BigDecimal amount, CurrencyType currency, BigDecimal exchangeRate, String rateSource) {

    public static DisplayPrice of(CurrencyConversionService conversionService, BigDecimal amount,
                                  CurrencyType originalCurrency, CurrencyType targetCurrency) {
        if (targetCurrency == null || amount == null) {
            return null;
        }
        CurrencyType original = originalCurrency != null ? originalCurrency : CurrencyType.TRY;
        if (original.equals(targetCurrency)) {
            return new DisplayPrice(amount, targetCurrency, BigDecimal.ONE, "LIVE");
        }
        ConversionResult result = conversionService.convert(amount, original, targetCurrency);
        return new DisplayPrice(result.convertedAmount(), targetCurrency, result.exchangeRate(),
                result.source().getDisplayName());
    }
}
