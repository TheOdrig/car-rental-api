package com.akif.currency.internal.service.exchangeRate.impl;

import com.akif.currency.internal.config.ExchangeRateClientConfig;
import com.akif.currency.internal.dto.exchangeRate.ExchangeRateApiResponse;
import com.akif.currency.internal.dto.exchangeRate.ExchangeRateResponse;
import com.akif.shared.enums.CurrencyType;
import com.akif.currency.api.RateSource;
import com.akif.currency.internal.exception.ExchangeRateApiException;
import com.akif.currency.internal.service.exchangeRate.ExchangeRateClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

@Service
@Slf4j
public class ExchangeRateClientImpl implements ExchangeRateClient {

    private final RestClient exchangeRateRestClient;

    private final String apiKey;

    private final Clock clock;

    public ExchangeRateClientImpl(RestClient exchangeRateRestClient,
                                   Clock clock,
                                   ExchangeRateClientConfig config) {
        this.exchangeRateRestClient = exchangeRateRestClient;
        this.apiKey = config.getApiKey();
        this.clock = clock;
    }

    @Override
    public ExchangeRateResponse fetchRates(CurrencyType baseCurrency) {
        log.info("Fetching exchange rates for base currency: {}", baseCurrency.getCode());

        try {
            ExchangeRateApiResponse apiResponse = exchangeRateRestClient
                    .get()
                    .uri("/{apiKey}/latest/{base}", apiKey, baseCurrency.getCode())
                    .retrieve()
                    .body(ExchangeRateApiResponse.class);

            if (apiResponse == null || apiResponse.rates() == null) {
                throw new ExchangeRateApiException("Empty response from Exchange Rate API");
            }

            ExchangeRateResponse response = mapToInternalResponse(apiResponse, baseCurrency);
            
            log.info("Successfully fetched {} exchange rates for base: {}", 
                    response.rates().size(), baseCurrency.getCode());
            
            return response;

        } catch (RestClientException e) {
            log.error("Failed to fetch exchange rates: {}", e.getMessage());
            throw new ExchangeRateApiException("Failed to fetch exchange rates", e);
        }
    }

    private ExchangeRateResponse mapToInternalResponse(ExchangeRateApiResponse apiResponse, 
                                                        CurrencyType baseCurrency) {
        Map<CurrencyType, BigDecimal> rates = new EnumMap<>(CurrencyType.class);

        for (CurrencyType currency : CurrencyType.values()) {
            BigDecimal rate = apiResponse.getRate(currency.getCode());
            if (rate != null) {
                rates.put(currency, rate);
            }
        }

        LocalDateTime timestamp = apiResponse.timeLastUpdated() != null
                ? LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(apiResponse.timeLastUpdated()),
                        clock.getZone())
                : LocalDateTime.now(clock);

        return new ExchangeRateResponse(
                baseCurrency,
                timestamp,
                rates,
                RateSource.LIVE
        );
    }
}
