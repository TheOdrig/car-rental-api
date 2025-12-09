package com.akif.service.currency;

import com.akif.dto.currency.ExchangeRateResponse;
import com.akif.shared.enums.CurrencyType;

public interface IExchangeRateClient {

    ExchangeRateResponse fetchRates(CurrencyType baseCurrency);
}
