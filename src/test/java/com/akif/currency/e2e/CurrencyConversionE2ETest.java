package com.akif.currency.e2e;

import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.currency.internal.exception.ExchangeRateApiException;
import com.akif.currency.internal.service.exchangeRate.ExchangeRateClient;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.internal.dto.request.RentalRequest;
import com.akif.shared.enums.CurrencyType;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@DisplayName("Currency Conversion Integration E2E Tests")
class CurrencyConversionE2ETest extends E2ETestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Nested
    @DisplayName("USD Conversion Tests")
    class UsdConversionTests {

        @Test
        @DisplayName("Should convert rental prices from TRY to USD when currency parameter is provided")
        void shouldConvertPricesFromTryToUsd_whenCurrencyParameterProvided() throws Exception {
            User testUser = TestDataBuilder.createTestUser("usduser");
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            String responseJson = mockMvc.perform(get("/api/rentals/{id}", rentalId)
                            .header("Authorization", "Bearer " + userToken)
                            .param("currency", "USD")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andExpect(jsonPath("$.displayCurrency").value("USD"))
                    .andExpect(jsonPath("$.convertedTotalPrice").exists())
                    .andExpect(jsonPath("$.exchangeRate").exists())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RentalResponse response = objectMapper.readValue(responseJson, RentalResponse.class);

            assertThat(response.displayCurrency()).isEqualTo(CurrencyType.USD);
            assertThat(response.convertedTotalPrice()).isNotNull();
            assertThat(response.exchangeRate()).isNotNull();
            assertThat(response.exchangeRate()).isGreaterThan(BigDecimal.ZERO);

            assertThat(response.totalPrice()).isNotNull();
            assertThat(response.currency()).isEqualTo(CurrencyType.TRY);
        }
    }

    @Nested
    @DisplayName("Dual Price Display Tests")
    class DualPriceDisplayTests {

        @Test
        @DisplayName("Should return both original and converted prices when currency parameter is provided")
        void shouldReturnBothOriginalAndConvertedPrices_whenCurrencyParameterProvided() throws Exception {
            User testUser = TestDataBuilder.createTestUser("dualpriceuser1");
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            String responseJson = mockMvc.perform(get("/api/rentals/{id}", rentalId)
                            .header("Authorization", "Bearer " + userToken)
                            .param("currency", "EUR")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andExpect(jsonPath("$.totalPrice").exists())
                    .andExpect(jsonPath("$.currency").value("TRY"))
                    .andExpect(jsonPath("$.convertedTotalPrice").exists())
                    .andExpect(jsonPath("$.displayCurrency").value("EUR"))
                    .andExpect(jsonPath("$.exchangeRate").exists())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RentalResponse response = objectMapper.readValue(responseJson, RentalResponse.class);

            assertThat(response.totalPrice()).isNotNull();
            assertThat(response.currency()).isEqualTo(CurrencyType.TRY);
            
            assertThat(response.convertedTotalPrice()).isNotNull();
            assertThat(response.displayCurrency()).isEqualTo(CurrencyType.EUR);
            
            assertThat(response.exchangeRate()).isNotNull();
            assertThat(response.exchangeRate()).isGreaterThan(BigDecimal.ZERO);

            assertThat(response.convertedTotalPrice()).isNotEqualTo(response.totalPrice());
        }

        @Test
        @DisplayName("Should return same price when converting to same currency")
        void shouldReturnSamePrice_whenConvertingToSameCurrency() throws Exception {
            User testUser = TestDataBuilder.createTestUser("dualpriceuser2");
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            String responseJson = mockMvc.perform(get("/api/rentals/{id}", rentalId)
                            .header("Authorization", "Bearer " + userToken)
                            .param("currency", "TRY")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RentalResponse response = objectMapper.readValue(responseJson, RentalResponse.class);

            assertThat(response.displayCurrency()).isEqualTo(CurrencyType.TRY);
            assertThat(response.convertedTotalPrice()).isEqualTo(response.totalPrice());
            assertThat(response.exchangeRate()).isEqualTo(BigDecimal.ONE);
        }
    }

    @Nested
    @DisplayName("Fallback Rates Tests")
    class FallbackRatesTests {

        @MockitoBean
        private ExchangeRateClient exchangeRateClient;

        @Test
        @DisplayName("Should use fallback rates when exchange rate API is unavailable")
        void shouldUseFallbackRates_whenExchangeRateApiUnavailable() throws Exception {
            User testUser = TestDataBuilder.createTestUser("fallbackuser1");
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            when(exchangeRateClient.fetchRates(any()))
                    .thenThrow(new ExchangeRateApiException("API unavailable"));

            String responseJson = mockMvc.perform(get("/api/rentals/{id}", rentalId)
                            .header("Authorization", "Bearer " + userToken)
                            .param("currency", "USD")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andExpect(jsonPath("$.displayCurrency").value("USD"))
                    .andExpect(jsonPath("$.convertedTotalPrice").exists())
                    .andExpect(jsonPath("$.exchangeRate").exists())
                    .andExpect(jsonPath("$.rateSource").exists())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RentalResponse response = objectMapper.readValue(responseJson, RentalResponse.class);

            assertThat(response.rateSource()).isEqualTo("Fallback");
            assertThat(response.convertedTotalPrice()).isNotNull();
            assertThat(response.exchangeRate()).isNotNull();
            assertThat(response.exchangeRate()).isGreaterThan(BigDecimal.ZERO);

            assertThat(response.displayCurrency()).isEqualTo(CurrencyType.USD);
        }

        @Test
        @DisplayName("Should include warning flag in response when fallback rates are used")
        void shouldIncludeWarningFlag_whenFallbackRatesUsed() throws Exception {
            User testUser = TestDataBuilder.createTestUser("fallbackuser2");
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            when(exchangeRateClient.fetchRates(any()))
                    .thenThrow(new ExchangeRateApiException("API unavailable"));

            String responseJson = mockMvc.perform(get("/api/rentals/{id}", rentalId)
                            .header("Authorization", "Bearer " + userToken)
                            .param("currency", "GBP")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rateSource").value("Fallback"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RentalResponse response = objectMapper.readValue(responseJson, RentalResponse.class);

            assertThat(response.rateSource()).isNotNull();
            assertThat(response.rateSource()).isEqualTo("Fallback");

            assertThat(response.convertedTotalPrice()).isNotNull();
            assertThat(response.displayCurrency()).isEqualTo(CurrencyType.GBP);
        }
    }

    @Nested
    @DisplayName("Multiple Currency Display Tests")
    class MultipleCurrencyDisplayTests {

        @Test
        @DisplayName("Should display rental price in multiple currencies")
        void shouldDisplayRentalPriceInMultipleCurrencies() throws Exception {
            User testUser = TestDataBuilder.createTestUser("multicurrencyuser");
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            mockMvc.perform(get("/api/rentals/{id}", rentalId)
                            .header("Authorization", "Bearer " + userToken)
                            .param("currency", "USD")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayCurrency").value("USD"))
                    .andExpect(jsonPath("$.convertedTotalPrice").exists())
                    .andExpect(jsonPath("$.exchangeRate").exists());

            mockMvc.perform(get("/api/rentals/{id}", rentalId)
                            .header("Authorization", "Bearer " + userToken)
                            .param("currency", "EUR")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayCurrency").value("EUR"))
                    .andExpect(jsonPath("$.convertedTotalPrice").exists())
                    .andExpect(jsonPath("$.exchangeRate").exists());

            mockMvc.perform(get("/api/rentals/{id}", rentalId)
                            .header("Authorization", "Bearer " + userToken)
                            .param("currency", "GBP")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayCurrency").value("GBP"))
                    .andExpect(jsonPath("$.convertedTotalPrice").exists())
                    .andExpect(jsonPath("$.exchangeRate").exists());

            mockMvc.perform(get("/api/rentals/{id}", rentalId)
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currency").value("TRY"))
                    .andExpect(jsonPath("$.totalPrice").exists());
        }
    }
}
