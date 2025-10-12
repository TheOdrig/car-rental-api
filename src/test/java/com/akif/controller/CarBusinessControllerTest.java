package com.akif.controller;

import com.akif.dto.request.CarPriceUpdateRequestDto;
import com.akif.dto.request.CarStatusUpdateRequestDto;
import com.akif.dto.response.CarResponseDto;
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
import static org.mockito.ArgumentMatchers.eq;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CarBusinessController.class)
@ContextConfiguration(classes = {CarBusinessController.class, SecurityConfig.class})
@DisplayName("CarBusinessController Unit Tests")
public class CarBusinessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ICarService carService;

    @Autowired
    private ObjectMapper objectMapper;


    private CarStatusUpdateRequestDto testStatusUpdateRequest;
    private CarPriceUpdateRequestDto testPriceUpdateRequest;

    @BeforeEach
    void setUp() {
        testStatusUpdateRequest = CarStatusUpdateRequestDto.builder()
                .carStatusType(CarStatusType.SOLD)
                .reason("Car sold to customer")
                .build();

        testPriceUpdateRequest = CarPriceUpdateRequestDto.builder()
                .price(new BigDecimal("240000"))
                .currencyType(CurrencyType.TRY)
                .damagePrice(BigDecimal.ZERO)
                .build();
    }

    @Nested
    @DisplayName("POST /api/cars/business/{id}/sell")
    class SellCarTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should sell car when valid ID provided")
        void shouldSellCarWhenValidIdProvided() throws Exception {

            Long carId = 1L;
            CarResponseDto soldCar = CarResponseDto.builder()
                    .id(carId)
                    .licensePlate("34ABC123")
                    .carStatusType(CarStatusType.SOLD)
                    .build();
            when(carService.sellCar(carId)).thenReturn(soldCar);

            mockMvc.perform(post("/api/cars/business/{id}/sell", carId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(carId))
                    .andExpect(jsonPath("$.carStatusType").value("Sold"));

            verify(carService).sellCar(carId);
        }
    }

    @Nested
    @DisplayName("POST /api/cars/business/{id}/reserve")
    class ReserveCarTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should reserve car when valid ID provided")
        void shouldReserveCarWhenValidIdProvided() throws Exception {

            Long carId = 1L;
            CarResponseDto reservedCar = CarResponseDto.builder()
                    .id(carId)
                    .licensePlate("34ABC123")
                    .carStatusType(CarStatusType.RESERVED)
                    .build();
            when(carService.reserveCar(carId)).thenReturn(reservedCar);

            mockMvc.perform(post("/api/cars/business/{id}/reserve", carId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(carId))
                    .andExpect(jsonPath("$.carStatusType").value("Reserved"));

            verify(carService).reserveCar(carId);
        }
    }

    @Nested
    @DisplayName("PATCH /api/cars/business/{id}/status")
    class UpdateCarStatusTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update car status when valid data provided")
        void shouldUpdateCarStatusWhenValidDataProvided() throws Exception {

            Long carId = 1L;
            CarResponseDto updatedCar = CarResponseDto.builder()
                    .id(carId)
                    .licensePlate("34ABC123")
                    .carStatusType(CarStatusType.SOLD)
                    .build();
            when(carService.updateCarStatus(eq(carId), any(CarStatusUpdateRequestDto.class))).thenReturn(updatedCar);

            mockMvc.perform(patch("/api/cars/business/{id}/status", carId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testStatusUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(carId))
                    .andExpect(jsonPath("$.carStatusType").value("Sold"));

            verify(carService).updateCarStatus(eq(carId), any(CarStatusUpdateRequestDto.class));
        }
    }

    @Nested
    @DisplayName("PATCH /api/cars/business/{id}/price")
    class UpdateCarPriceTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update car price when valid data provided")
        void shouldUpdateCarPriceWhenValidDataProvided() throws Exception {

            Long carId = 1L;
            CarResponseDto updatedCar = CarResponseDto.builder()
                    .id(carId)
                    .licensePlate("34ABC123")
                    .price(new BigDecimal("240000"))
                    .build();
            when(carService.updateCarPrice(eq(carId), any(CarPriceUpdateRequestDto.class))).thenReturn(updatedCar);

            mockMvc.perform(patch("/api/cars/business/{id}/price", carId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testPriceUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(carId))
                    .andExpect(jsonPath("$.price").value(240000));

            verify(carService).updateCarPrice(eq(carId), any(CarPriceUpdateRequestDto.class));
        }
    }

    @Nested
    @DisplayName("POST /api/cars/business/{id}/view")
    class IncrementViewCountTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should increment view count when valid ID provided")
        void shouldIncrementViewCountWhenValidIdProvided() throws Exception {

            Long carId = 1L;
            doNothing().when(carService).incrementViewCount(carId);

            mockMvc.perform(post("/api/cars/business/{id}/view", carId))
                    .andExpect(status().isOk());

            verify(carService).incrementViewCount(carId);
        }
    }

    @Nested
    @DisplayName("GET /api/cars/business/{id}/can-sell")
    class CanCarBeSoldTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return can sell status")
        void shouldReturnCanSellStatus() throws Exception {

            Long carId = 1L;
            when(carService.canCarBeSold(carId)).thenReturn(true);

            mockMvc.perform(get("/api/cars/business/{id}/can-sell", carId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.canBeSold").value(true));

            verify(carService).canCarBeSold(carId);
        }
    }
}


