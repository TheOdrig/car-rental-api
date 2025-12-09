package com.akif.controller;

import com.akif.dto.currency.ConvertRequest;
import com.akif.shared.enums.CurrencyType;
import com.akif.starter.CarGalleryProjectApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CurrencyController Integration Tests")
class CurrencyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("GET /api/exchange-rates")
    class GetAllRates {

        @Test
        @WithMockUser
        @DisplayName("Should return all exchange rates")
        void shouldReturnAllExchangeRates() throws Exception {
            mockMvc.perform(get("/api/exchange-rates"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.baseCurrency").exists())
                    .andExpect(jsonPath("$.rates").exists())
                    .andExpect(jsonPath("$.source").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/exchange-rates/{from}/{to}")
    class GetRate {

        @Test
        @WithMockUser
        @DisplayName("Should return exchange rate between USD and TRY")
        void shouldReturnExchangeRateBetweenUsdAndTry() throws Exception {
            mockMvc.perform(get("/api/exchange-rates/USD/TRY"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.from").value("USD"))
                    .andExpect(jsonPath("$.to").value("TRY"))
                    .andExpect(jsonPath("$.rate").isNumber())
                    .andExpect(jsonPath("$.source").exists());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return rate 1.0 for same currency")
        void shouldReturnRateOneForSameCurrency() throws Exception {
            mockMvc.perform(get("/api/exchange-rates/USD/USD"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rate").value(1));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 for invalid currency code")
        void shouldReturn400ForInvalidCurrencyCode() throws Exception {
            mockMvc.perform(get("/api/exchange-rates/INVALID/TRY"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/exchange-rates/convert")
    class Convert {

        @Test
        @WithMockUser
        @DisplayName("Should convert USD to TRY successfully")
        void shouldConvertUsdToTrySuccessfully() throws Exception {
            ConvertRequest request = new ConvertRequest(
                    new BigDecimal("100.00"),
                    CurrencyType.USD,
                    CurrencyType.TRY
            );

            mockMvc.perform(post("/api/exchange-rates/convert")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.originalAmount").value(100.00))
                    .andExpect(jsonPath("$.originalCurrency").value("USD"))
                    .andExpect(jsonPath("$.targetCurrency").value("TRY"))
                    .andExpect(jsonPath("$.convertedAmount").isNumber())
                    .andExpect(jsonPath("$.exchangeRate").isNumber())
                    .andExpect(jsonPath("$.source").exists());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return same amount for same currency conversion")
        void shouldReturnSameAmountForSameCurrencyConversion() throws Exception {
            ConvertRequest request = new ConvertRequest(
                    new BigDecimal("100.00"),
                    CurrencyType.USD,
                    CurrencyType.USD
            );

            mockMvc.perform(post("/api/exchange-rates/convert")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.originalAmount").value(100.00))
                    .andExpect(jsonPath("$.convertedAmount").value(100.00))
                    .andExpect(jsonPath("$.exchangeRate").value(1));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 for null amount")
        void shouldReturn400ForNullAmount() throws Exception {
            String invalidRequest = """
                    {
                        "amount": null,
                        "fromCurrency": "USD",
                        "toCurrency": "TRY"
                    }
                    """;

            mockMvc.perform(post("/api/exchange-rates/convert")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 for missing currency")
        void shouldReturn400ForMissingCurrency() throws Exception {
            String invalidRequest = """
                    {
                        "amount": 100.00,
                        "fromCurrency": "USD"
                    }
                    """;

            mockMvc.perform(post("/api/exchange-rates/convert")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/exchange-rates/refresh")
    class RefreshRates {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should refresh rates when admin")
        void shouldRefreshRatesWhenAdmin() throws Exception {
            mockMvc.perform(post("/api/exchange-rates/refresh")
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return 403 when not admin")
        void shouldReturn403WhenNotAdmin() throws Exception {
            mockMvc.perform(post("/api/exchange-rates/refresh")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when not authenticated")
        void shouldReturn403WhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/exchange-rates/refresh")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
}
