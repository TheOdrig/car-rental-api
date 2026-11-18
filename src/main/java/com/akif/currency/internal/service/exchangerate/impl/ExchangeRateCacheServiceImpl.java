package com.akif.currency.internal.service.exchangerate.impl;

import java.time.Clock;

import com.akif.shared.config.CacheConfig;
import com.akif.currency.internal.config.FallbackRatesConfig;
import com.akif.currency.internal.dto.exchangerate.ExchangeRateResponse;
import com.akif.shared.enums.CurrencyType;
import com.akif.currency.api.RateSource;
import com.akif.currency.internal.exception.ExchangeRateApiException;
import com.akif.currency.internal.service.exchangerate.ExchangeRateCacheService;
import com.akif.currency.internal.service.exchangerate.ExchangeRateClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateCacheServiceImpl implements ExchangeRateCacheService {

    private final Clock clock;

    private static final CurrencyType BASE_CURRENCY = CurrencyType.USD;

    private final ExchangeRateClient exchangeRateClient;
    private final FallbackRatesConfig fallbackRatesConfig;

    @Override
    @Cacheable(value = CacheConfig.EXCHANGE_RATES_CACHE, key = "#baseCurrency")
    public ExchangeRateResponse getCachedRates(CurrencyType baseCurrency) {
        log.debug("Cache miss - fetching rates for base: {}", baseCurrency);
        return fetchRatesFromApi(baseCurrency);
    }

    @Override
    @CacheEvict(value = CacheConfig.EXCHANGE_RATES_CACHE, allEntries = true)
    public void evictCache() {
        log.info("Exchange rates cache evicted");
    }

    private ExchangeRateResponse fetchRatesFromApi(CurrencyType baseCurrency) {
        try {
            ExchangeRateResponse response = exchangeRateClient.fetchRates(baseCurrency);
            log.info("Fetched {} rates from API for base: {}", response.rates().size(), baseCurrency);
            return response;
        } catch (ExchangeRateApiException e) {
            log.warn("API call failed, using fallback rates: {}", e.getMessage());
            return createFallbackResponse(baseCurrency);
        }
    }

    private ExchangeRateResponse createFallbackResponse(CurrencyType baseCurrency) {
        log.warn("Using fallback rates for base: {}", baseCurrency);

        Map<CurrencyType, BigDecimal> configRates = fallbackRatesConfig.getRates();
        Map<CurrencyType, BigDecimal> rates = new EnumMap<>(CurrencyType.class);

        if (baseCurrency == BASE_CURRENCY) {
            rates.putAll(configRates);
        } else {
            BigDecimal baseToUsd = configRates.get(baseCurrency);
            if (baseToUsd != null && baseToUsd.compareTo(BigDecimal.ZERO) > 0) {
                for (Map.Entry<CurrencyType, BigDecimal> entry : configRates.entrySet()) {
                    BigDecimal rate = entry.getValue().divide(baseToUsd, 6, RoundingMode.HALF_UP);
                    rates.put(entry.getKey(), rate);
                }
            }
        }

        return new ExchangeRateResponse(baseCurrency, LocalDateTime.now(clock), rates, RateSource.FALLBACK);
    }
}
