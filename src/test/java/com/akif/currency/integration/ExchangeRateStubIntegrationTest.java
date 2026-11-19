package com.akif.currency.integration;

import com.akif.CarRentalApplication;
import com.akif.currency.api.RateSource;
import com.akif.currency.internal.dto.exchangerate.ExchangeRateResponse;
import com.akif.currency.internal.service.exchangerate.ExchangeRateClient;
import com.akif.shared.enums.CurrencyType;
import com.akif.testsupport.FakeExchangeRateServer;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Exchange rate client in the test profile")
class ExchangeRateStubIntegrationTest {

    @Autowired
    private ExchangeRateClient exchangeRateClient;

    @Value("${exchange-rate.api.base-url}")
    private String baseUrl;

    @Test
    @DisplayName("talks to the local stub and never to the real rates provider")
    void usesTheLocalStub() {
        ExchangeRateResponse response = exchangeRateClient.fetchRates(CurrencyType.USD);

        assertThat(baseUrl).isEqualTo(FakeExchangeRateServer.instance().baseUrl()).startsWith("http://127.0.0.1:");
        assertThat(response.source()).isEqualTo(RateSource.LIVE);
        assertThat(response.getRate(CurrencyType.TRY)).isEqualByComparingTo("42.49");
    }

    @Test
    @DisplayName("derives the rates of another base currency from the same table")
    void derivesOtherBases() {
        ExchangeRateResponse response = exchangeRateClient.fetchRates(CurrencyType.TRY);

        assertThat(response.getRate(CurrencyType.USD)).isEqualByComparingTo("0.023535");
    }
}
