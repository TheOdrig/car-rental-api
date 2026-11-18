package com.akif.currency.internal.service.exchangerate;

import com.akif.currency.internal.dto.exchangerate.ExchangeRateResponse;
import com.akif.shared.enums.CurrencyType;

public interface ExchangeRateClient {

    ExchangeRateResponse fetchRates(CurrencyType baseCurrency);
}
