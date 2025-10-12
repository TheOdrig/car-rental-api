package com.akif.controller;

import com.akif.dto.request.CarRequestDto;
import com.akif.enums.CarStatusType;
import com.akif.enums.CurrencyType;
import com.akif.service.ICarService;
import com.akif.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;

import static org.mockito.ArgumentMatchers.any;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(CarStatisticsController.class)
@ContextConfiguration(classes = {CarStatisticsController.class, SecurityConfig.class})
@DisplayName("CarStatisticsController Unit Tests")
public class CarStatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ICarService carService;

    @Autowired
    private ObjectMapper objectMapper;

    private CarRequestDto testCarRequestDto;
    private Map<String, Object> testStatistics;
    private Map<String, Long> testStatusCounts;
    private Map<String, Long> testBrandCounts;
    private Map<String, BigDecimal> testAveragePrices;


    @BeforeEach
    void setUp() {
        testCarRequestDto = CarRequestDto.builder()
                .licensePlate("34ABC123")
                .vinNumber("1HGBH41JXMN109186")
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2020)
                .price(new BigDecimal("250000"))
                .currencyType(CurrencyType.TRY)
                .damagePrice(BigDecimal.ZERO)
                .carStatusType(CarStatusType.AVAILABLE)
                .isFeatured(true)
                .isTestDriveAvailable(true)
                .build();

        testStatistics = new HashMap<>();
        testStatistics.put("totalCars", 100L);
        testStatistics.put("activeCars", 95L);
        testStatistics.put("averagePrice", new BigDecimal("275000"));

        testStatusCounts = new HashMap<>();
        testStatusCounts.put("AVAILABLE", 80L);
        testStatusCounts.put("SOLD", 15L);
        testStatusCounts.put("RESERVED", 5L);

        testBrandCounts = new HashMap<>();
        testBrandCounts.put("Toyota", 30L);
        testBrandCounts.put("Honda", 25L);
        testBrandCounts.put("BMW", 20L);

        testAveragePrices = new HashMap<>();
        testAveragePrices.put("Toyota", new BigDecimal("250000"));
        testAveragePrices.put("Honda", new BigDecimal("280000"));
        testAveragePrices.put("BMW", new BigDecimal("350000"));
    }

    @Nested
    @DisplayName("GET /api/cars/statistics")
    class GetCarStatisticsTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return car statistics")
        void shouldReturnCarStatistics() throws Exception {

            when(carService.getCarStatistics()).thenReturn(testStatistics);

            mockMvc.perform(get("/api/cars/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalCars").value(100))
                    .andExpect(jsonPath("$.activeCars").value(95))
                    .andExpect(jsonPath("$.averagePrice").value(275000));

            verify(carService).getCarStatistics();
        }
    }

    @Nested
    @DisplayName("GET /api/cars/statistics/status-counts")
    class GetCarsCountByStatusTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return cars count by status")
        void shouldReturnCarsCountByStatus() throws Exception {

            when(carService.getCarsCountByStatus()).thenReturn(testStatusCounts);

            mockMvc.perform(get("/api/cars/statistics/status-counts"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.AVAILABLE").value(80))
                    .andExpect(jsonPath("$.SOLD").value(15))
                    .andExpect(jsonPath("$.RESERVED").value(5));

            verify(carService).getCarsCountByStatus();
        }
    }

    @Nested
    @DisplayName("GET /api/cars/statistics/brand-counts")
    class GetCarsCountByBrandTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return cars count by brand")
        void shouldReturnCarsCountByBrand() throws Exception {

            when(carService.getCarsCountByBrand()).thenReturn(testBrandCounts);

            mockMvc.perform(get("/api/cars/statistics/brand-counts"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.Toyota").value(30))
                    .andExpect(jsonPath("$.Honda").value(25))
                    .andExpect(jsonPath("$.BMW").value(20));

            verify(carService).getCarsCountByBrand();
        }
    }

    @Nested
    @DisplayName("GET /api/cars/statistics/average-prices")
    class GetAveragePriceByBrandTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return average prices by brand")
        void shouldReturnAveragePricesByBrand() throws Exception {

            when(carService.getAveragePriceByBrand()).thenReturn(testAveragePrices);

            mockMvc.perform(get("/api/cars/statistics/average-prices"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.Toyota").value(250000))
                    .andExpect(jsonPath("$.Honda").value(280000))
                    .andExpect(jsonPath("$.BMW").value(350000));

            verify(carService).getAveragePriceByBrand();
        }
    }

    @Nested
    @DisplayName("GET /api/cars/statistics/count")
    class GetCarCountTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return total car count")
        void shouldReturnTotalCarCount() throws Exception {

            when(carService.getCarCount()).thenReturn(100L);

            mockMvc.perform(get("/api/cars/statistics/count"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.count").value(100));

            verify(carService).getCarCount();
        }
    }

    @Nested
    @DisplayName("POST /api/cars/statistics/validate")
    class ValidateCarDataTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should validate car data successfully")
        void shouldValidateCarDataSuccessfully() throws Exception {

            when(carService.validateCarData(any(CarRequestDto.class))).thenReturn(List.of());

            mockMvc.perform(post("/api/cars/statistics/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testCarRequestDto)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.isValid").value(true))
                    .andExpect(jsonPath("$.errors").isArray());

            verify(carService).validateCarData(any(CarRequestDto.class));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return validation errors when invalid data provided")
        void shouldReturnValidationErrorsWhenInvalidDataProvided() throws Exception {

            CarRequestDto invalidRequest = CarRequestDto.builder()
                    .licensePlate("")
                    .build();

            mockMvc.perform(post("/api/cars/statistics/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

}
