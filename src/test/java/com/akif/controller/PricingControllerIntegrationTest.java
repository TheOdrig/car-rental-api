package com.akif.controller;

import com.akif.dto.request.PricingRequestDto;
import com.akif.enums.CarStatusType;
import com.akif.enums.CurrencyType;
import com.akif.model.Car;
import com.akif.repository.CarRepository;
import com.akif.starter.CarGalleryProjectApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("PricingController Integration Tests")
class PricingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Car testCar;

    @BeforeEach
    void setUp() {
        carRepository.deleteAll();

        testCar = Car.builder()
                .licensePlate("34TEST123")
                .vinNumber("1HGBH41JXMN109999")
                .brand("Toyota")
                .model("Camry")
                .productionYear(2023)
                .price(new BigDecimal("500"))
                .currencyType(CurrencyType.TRY)
                .damagePrice(BigDecimal.ZERO)
                .carStatusType(CarStatusType.AVAILABLE)
                .bodyType("Sedan")
                .isFeatured(false)
                .isTestDriveAvailable(false)
                .viewCount(0L)
                .likeCount(0L)
                .build();
        testCar = carRepository.save(testCar);
    }

    @AfterEach
    void tearDown() {
        carRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /api/pricing/calculate")
    class CalculatePrice {

        @Test
        @WithMockUser
        @DisplayName("Should calculate price successfully")
        void shouldCalculatePriceSuccessfully() throws Exception {
            PricingRequestDto request = new PricingRequestDto(
                    testCar.getId(),
                    LocalDate.now().plusDays(10),
                    LocalDate.now().plusDays(15)
            );

            mockMvc.perform(post("/api/pricing/calculate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.basePrice").value(500))
                    .andExpect(jsonPath("$.rentalDays").value(6))
                    .andExpect(jsonPath("$.finalPrice").isNumber())
                    .andExpect(jsonPath("$.appliedModifiers").isArray());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when car not found")
        void shouldReturn404WhenCarNotFound() throws Exception {
            PricingRequestDto request = new PricingRequestDto(
                    99999L,
                    LocalDate.now().plusDays(10),
                    LocalDate.now().plusDays(15)
            );

            mockMvc.perform(post("/api/pricing/calculate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 for invalid date range")
        void shouldReturn400ForInvalidDateRange() throws Exception {
            PricingRequestDto request = new PricingRequestDto(
                    testCar.getId(),
                    LocalDate.now().plusDays(15),
                    LocalDate.now().plusDays(10)
            );

            mockMvc.perform(post("/api/pricing/calculate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("Should apply early booking discount for advance booking")
        void shouldApplyEarlyBookingDiscountForAdvanceBooking() throws Exception {

            PricingRequestDto request = new PricingRequestDto(
                    testCar.getId(),
                    LocalDate.now().plusDays(35),
                    LocalDate.now().plusDays(40)
            );

            mockMvc.perform(post("/api/pricing/calculate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.appliedModifiers").isArray())
                    .andExpect(jsonPath("$.totalSavings").exists());
        }

        @Test
        @WithMockUser
        @DisplayName("Should apply duration discount for long rental")
        void shouldApplyDurationDiscountForLongRental() throws Exception {

            PricingRequestDto request = new PricingRequestDto(
                    testCar.getId(),
                    LocalDate.now().plusDays(10),
                    LocalDate.now().plusDays(20)
            );

            mockMvc.perform(post("/api/pricing/calculate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rentalDays").value(11))
                    .andExpect(jsonPath("$.appliedModifiers").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/pricing/preview")
    class PreviewPrice {

        @Test
        @WithMockUser
        @DisplayName("Should preview price successfully")
        void shouldPreviewPriceSuccessfully() throws Exception {
            mockMvc.perform(get("/api/pricing/preview")
                            .param("carId", testCar.getId().toString())
                            .param("startDate", LocalDate.now().plusDays(10).toString())
                            .param("endDate", LocalDate.now().plusDays(15).toString()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.basePrice").value(500))
                    .andExpect(jsonPath("$.rentalDays").value(6))
                    .andExpect(jsonPath("$.finalPrice").isNumber());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when car not found")
        void shouldReturn404WhenCarNotFound() throws Exception {
            mockMvc.perform(get("/api/pricing/preview")
                            .param("carId", "99999")
                            .param("startDate", LocalDate.now().plusDays(10).toString())
                            .param("endDate", LocalDate.now().plusDays(15).toString()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 for missing parameters")
        void shouldReturn400ForMissingParameters() throws Exception {
            mockMvc.perform(get("/api/pricing/preview")
                            .param("carId", testCar.getId().toString()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 for invalid date format")
        void shouldReturn400ForInvalidDateFormat() throws Exception {
            mockMvc.perform(get("/api/pricing/preview")
                            .param("carId", testCar.getId().toString())
                            .param("startDate", "invalid-date")
                            .param("endDate", LocalDate.now().plusDays(15).toString()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/pricing/strategies")
    class GetEnabledStrategies {

        @Test
        @DisplayName("Should return list of enabled strategies without authentication")
        void shouldReturnListOfEnabledStrategies() throws Exception {
            mockMvc.perform(get("/api/pricing/strategies"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].name").exists())
                    .andExpect(jsonPath("$[0].order").exists())
                    .andExpect(jsonPath("$[0].enabled").exists());
        }

        @Test
        @DisplayName("Should return strategies in correct order without authentication")
        void shouldReturnStrategiesInCorrectOrder() throws Exception {
            mockMvc.perform(get("/api/pricing/strategies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].order").value(1))
                    .andExpect(jsonPath("$[0].name").value("Season Pricing"));
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should return 403 when not authenticated for calculate")
        void shouldReturn403WhenNotAuthenticatedForCalculate() throws Exception {
            PricingRequestDto request = new PricingRequestDto(
                    testCar.getId(),
                    LocalDate.now().plusDays(10),
                    LocalDate.now().plusDays(15)
            );

            mockMvc.perform(post("/api/pricing/calculate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}
