package com.akif.currency.internal.dto.exchangeRate;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;

public record ExchangeRateApiResponse(
        @JsonProperty("base_code") String base,
        @JsonProperty("date") String date,
        @JsonProperty("time_last_update_unix") Long timeLastUpdated,
        @JsonProperty("conversion_rates") Map<String, BigDecimal> rates) {

    public boolean hasRate(String currencyCode) {
        return rates != null && rates.containsKey(currencyCode);
    }

    public BigDecimal getRate(String currencyCode) {
        if (rates == null) {
            return null;
        }
        return rates.get(currencyCode);
    }
}
