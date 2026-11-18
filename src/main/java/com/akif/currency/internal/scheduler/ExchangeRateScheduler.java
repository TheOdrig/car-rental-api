package com.akif.currency.internal.scheduler;

import com.akif.currency.api.CurrencyConversionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateScheduler {

    private final CurrencyConversionService currencyConversionService;

    @Scheduled(fixedRateString = "${currency.refresh.interval}")
    public void refreshExchangeRates() {
        log.info("Scheduled exchange rate refresh started");
        try {
            currencyConversionService.refreshRates();
            log.info("Scheduled exchange rate refresh completed successfully");
        } catch (Exception e) {
            log.error("Scheduled exchange rate refresh failed: {}", e.getMessage(), e);
        }
    }
}
