package com.akif.e2e.pricing;

import com.akif.dto.request.RentalRequestDto;
import com.akif.dto.response.RentalResponseDto;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.enums.CurrencyType;
import com.akif.exception.ExchangeRateApiException;
import com.akif.model.Car;
import com.akif.model.User;
import com.akif.repository.CarRepository;
import com.akif.repository.UserRepository;
import com.akif.service.currency.IExchangeRateClient;
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

            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
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

            RentalResponseDto response = objectMapper.readValue(responseJson, RentalResponseDto.class);

            assertThat(response.getDisplayCurrency()).isEqualTo(CurrencyType.USD);
            assertThat(response.getConvertedTotalPrice()).isNotNull();
            assertThat(response.getExchangeRate()).isNotNull();
            assertThat(response.getExchangeRate()).isGreaterThan(BigDecimal.ZERO);

            assertThat(response.getTotalPrice()).isNotNull();
            assertThat(response.getCurrency()).isEqualTo(CurrencyType.TRY);
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

            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
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

            RentalResponseDto response = objectMapper.readValue(responseJson, RentalResponseDto.class);

            assertThat(response.getTotalPrice()).isNotNull();
            assertThat(response.getCurrency()).isEqualTo(CurrencyType.TRY);
            
            assertThat(response.getConvertedTotalPrice()).isNotNull();
            assertThat(response.getDisplayCurrency()).isEqualTo(CurrencyType.EUR);
            
            assertThat(response.getExchangeRate()).isNotNull();
            assertThat(response.getExchangeRate()).isGreaterThan(BigDecimal.ZERO);

            assertThat(response.getConvertedTotalPrice()).isNotEqualTo(response.getTotalPrice());
        }

        @Test
        @DisplayName("Should return same price when converting to same currency")
        void shouldReturnSamePrice_whenConvertingToSameCurrency() throws Exception {
            User testUser = TestDataBuilder.createTestUser("dualpriceuser2");
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
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

            RentalResponseDto response = objectMapper.readValue(responseJson, RentalResponseDto.class);

            assertThat(response.getDisplayCurrency()).isEqualTo(CurrencyType.TRY);
            assertThat(response.getConvertedTotalPrice()).isEqualTo(response.getTotalPrice());
            assertThat(response.getExchangeRate()).isEqualTo(BigDecimal.ONE);
        }
    }

    @Nested
    @DisplayName("Fallback Rates Tests")
    class FallbackRatesTests {

        @MockitoBean
        private IExchangeRateClient exchangeRateClient;

        @Test
        @DisplayName("Should use fallback rates when exchange rate API is unavailable")
        void shouldUseFallbackRates_whenExchangeRateApiUnavailable() throws Exception {
            User testUser = TestDataBuilder.createTestUser("fallbackuser1");
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
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

            RentalResponseDto response = objectMapper.readValue(responseJson, RentalResponseDto.class);

            assertThat(response.getRateSource()).isEqualTo("Fallback");
            assertThat(response.getConvertedTotalPrice()).isNotNull();
            assertThat(response.getExchangeRate()).isNotNull();
            assertThat(response.getExchangeRate()).isGreaterThan(BigDecimal.ZERO);

            assertThat(response.getDisplayCurrency()).isEqualTo(CurrencyType.USD);
        }

        @Test
        @DisplayName("Should include warning flag in response when fallback rates are used")
        void shouldIncludeWarningFlag_whenFallbackRatesUsed() throws Exception {
            User testUser = TestDataBuilder.createTestUser("fallbackuser2");
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
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

            RentalResponseDto response = objectMapper.readValue(responseJson, RentalResponseDto.class);

            assertThat(response.getRateSource()).isNotNull();
            assertThat(response.getRateSource()).isEqualTo("Fallback");

            assertThat(response.getConvertedTotalPrice()).isNotNull();
            assertThat(response.getDisplayCurrency()).isEqualTo(CurrencyType.GBP);
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

            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
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
