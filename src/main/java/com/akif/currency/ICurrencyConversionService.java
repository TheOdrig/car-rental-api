package com.akif.currency;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;

public interface ICurrencyConversionService {

    ConversionResult convert(BigDecimal amount, CurrencyType from, CurrencyType to);

    ExchangeRate getRate(CurrencyType from, CurrencyType to);

    ExchangeRatesResponse getAllRates();

    void refreshRates();
}
