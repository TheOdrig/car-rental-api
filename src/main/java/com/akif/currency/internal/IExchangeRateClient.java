package com.akif.currency.internal;

import com.akif.currency.internal.dto.ExchangeRateResponse;
import com.akif.shared.enums.CurrencyType;

public interface IExchangeRateClient {

    ExchangeRateResponse fetchRates(CurrencyType baseCurrency);
}
