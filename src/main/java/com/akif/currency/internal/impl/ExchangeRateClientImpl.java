package com.akif.currency.internal.impl;

import com.akif.currency.internal.config.ExchangeRateClientConfig;
import com.akif.currency.internal.dto.ExchangeRateApiResponse;
import com.akif.currency.internal.dto.ExchangeRateResponse;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.RateSource;
import com.akif.exception.ExchangeRateApiException;
import com.akif.currency.internal.IExchangeRateClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;

@Service
@Slf4j
public class ExchangeRateClientImpl implements IExchangeRateClient {

    private final RestClient exchangeRateRestClient;

    private final String apiKey;

    public ExchangeRateClientImpl(RestClient exchangeRateRestClient, 
                                   ExchangeRateClientConfig config) {
        this.exchangeRateRestClient = exchangeRateRestClient;
        this.apiKey = config.getApiKey();
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
                        ZoneId.systemDefault())
                : LocalDateTime.now();

        return new ExchangeRateResponse(
                baseCurrency,
                timestamp,
                rates,
                RateSource.LIVE
        );
    }
}
