package com.akif.service.currency;

import com.akif.dto.currency.ExchangeRateResponse;
import com.akif.enums.CurrencyType;

public interface IExchangeRateCacheService {

    ExchangeRateResponse getCachedRates(CurrencyType baseCurrency);

    void evictCache();
}
