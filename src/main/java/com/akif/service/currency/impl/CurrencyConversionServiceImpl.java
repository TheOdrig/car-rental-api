package com.akif.service.currency.impl;

import com.akif.dto.currency.ConversionResult;
import com.akif.dto.currency.ExchangeRate;
import com.akif.dto.currency.ExchangeRateResponse;
import com.akif.dto.currency.ExchangeRatesResponse;
import com.akif.enums.CurrencyType;
import com.akif.enums.RateSource;
import com.akif.service.currency.ICurrencyConversionService;
import com.akif.service.currency.IExchangeRateCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyConversionServiceImpl implements ICurrencyConversionService {

    private static final CurrencyType BASE_CURRENCY = CurrencyType.USD;

    private final IExchangeRateCacheService cacheService;

    @Override
    public ConversionResult convert(BigDecimal amount, CurrencyType from, CurrencyType to) {
        log.debug("Converting {} {} to {}", amount, from, to);

        if (from.equals(to)) {
            return new ConversionResult(
                    amount,
                    from,
                    amount,
                    to,
                    BigDecimal.ONE,
                    LocalDateTime.now(),
                    RateSource.LIVE
            );
        }

        ExchangeRate rate = getRate(from, to);
        BigDecimal convertedAmount = calculateConversion(amount, rate.rate(), to);

        log.debug("Converted {} {} to {} {} (rate: {})",
                amount, from, convertedAmount, to, rate.rate());

        return new ConversionResult(
                amount,
                from,
                convertedAmount,
                to,
                rate.rate(),
                rate.timestamp(),
                rate.source()
        );
    }

    @Override
    public ExchangeRate getRate(CurrencyType from, CurrencyType to) {
        log.debug("Getting exchange rate from {} to {}", from, to);

        if (from.equals(to)) {
            return new ExchangeRate(from, to, BigDecimal.ONE, LocalDateTime.now(), RateSource.LIVE);
        }

        ExchangeRateResponse ratesResponse = cacheService.getCachedRates(from);
        BigDecimal rate = ratesResponse.getRate(to);

        if (rate == null) {
            log.warn("Rate not found for {} -> {}, using cross-rate calculation", from, to);
            rate = calculateCrossRate(from, to);
            return new ExchangeRate(from, to, rate, LocalDateTime.now(), ratesResponse.source());
        }

        return new ExchangeRate(from, to, rate, ratesResponse.timestamp(), ratesResponse.source());
    }

    @Override
    public ExchangeRatesResponse getAllRates() {
        log.debug("Getting all exchange rates");

        ExchangeRateResponse response = cacheService.getCachedRates(BASE_CURRENCY);

        return new ExchangeRatesResponse(
                response.baseCurrency(),
                response.rates(),
                response.timestamp(),
                response.source()
        );
    }

    @Override
    public void refreshRates() {
        log.info("Refreshing exchange rates");
        cacheService.evictCache();

        cacheService.getCachedRates(BASE_CURRENCY);
        log.info("Exchange rates refreshed successfully");
    }

    private BigDecimal calculateConversion(BigDecimal amount, BigDecimal rate, CurrencyType targetCurrency) {
        BigDecimal converted = amount.multiply(rate);
        return converted.setScale(targetCurrency.getDecimalPlaces(), RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCrossRate(CurrencyType from, CurrencyType to) {
        ExchangeRateResponse usdRates = cacheService.getCachedRates(BASE_CURRENCY);

        BigDecimal fromToUsd = usdRates.getRate(from);
        BigDecimal toToUsd = usdRates.getRate(to);

        if (fromToUsd == null || toToUsd == null || fromToUsd.compareTo(BigDecimal.ZERO) == 0) {
            log.error("Cannot calculate cross rate for {} -> {}", from, to);
            return BigDecimal.ONE;
        }

        return toToUsd.divide(fromToUsd, 6, RoundingMode.HALF_UP);
    }
}
