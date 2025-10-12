package com.akif.controller;

import com.akif.dto.request.CarRequestDto;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CarController.class)
@ContextConfiguration(classes = {CarController.class, SecurityConfig.class})
@DisplayName("CarController Unit Tests")
public class CarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ICarService carService;

    @Autowired
    private ObjectMapper objectMapper;

    private CarRequestDto testCarRequestDto;
    private CarResponseDto testCarResponseDto;

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

        testCarResponseDto = CarResponseDto.builder()
                .id(1L)
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
                .viewCount(0L)
                .likeCount(0L)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }


    @Nested
    @DisplayName("GET /api/cars/{id}")
    class GetCarByIdTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return car when valid ID provided")
        void shouldReturnCarWhenValidIdProvided() throws Exception {

            Long carId = 1L;
            when(carService.getCarById(carId)).thenReturn(testCarResponseDto);

            mockMvc.perform(get("/api/cars/{id}", carId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(carId))
                    .andExpect(jsonPath("$.licensePlate").value("34ABC123"))
                    .andExpect(jsonPath("$.brand").value("Toyota"))
                    .andExpect(jsonPath("$.model").value("Corolla"));

            verify(carService).getCarById(carId);
        }

    }

    @Nested
    @DisplayName("GET /api/cars/licensePlate/{licensePlate}")
    class GetCarByLicensePlateTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return car when valid license plate provided")
        void shouldReturnCarWhenValidLicensePlateProvided() throws Exception {

            String licensePlate = "34ABC123";
            when(carService.getCarByLicensePlate(licensePlate)).thenReturn(testCarResponseDto);

            mockMvc.perform(get("/api/cars/licensePlate/{licensePlate}", licensePlate))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.licensePlate").value(licensePlate))
                    .andExpect(jsonPath("$.brand").value("Toyota"));

            verify(carService).getCarByLicensePlate(licensePlate);
        }
    }

    @Nested
    @DisplayName("POST /api/cars")
    class CreateCarTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create car when valid data provided")
        void shouldCreateCarWhenValidDataProvided() throws Exception {

            when(carService.createCar(any(CarRequestDto.class))).thenReturn(testCarResponseDto);

            mockMvc.perform(post("/api/cars")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testCarRequestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.licensePlate").value("34ABC123"));

            verify(carService).createCar(any(CarRequestDto.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 400 when invalid data provided")
        void shouldReturn400WhenInvalidDataProvided() throws Exception {

            CarRequestDto invalidRequest = CarRequestDto.builder()
                    .licensePlate("")
                    .vinNumber("INVALID")
                    .brand("")
                    .model("")
                    .productionYear(null)
                    .price(null)
                    .currencyType(null)
                    .carStatusType(null)
                    .build();

            mockMvc.perform(post("/api/cars")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/cars/{id}")
    class UpdateCarTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update car when valid data provided")
        void shouldUpdateCarWhenValidDataProvided() throws Exception {

            Long carId = 1L;
            when(carService.updateCar(eq(carId), any(CarRequestDto.class))).thenReturn(testCarResponseDto);

            mockMvc.perform(put("/api/cars/{id}", carId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testCarRequestDto)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(carId));

            verify(carService).updateCar(eq(carId), any(CarRequestDto.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/cars/{id}")
    class DeleteCarTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete car when valid ID provided")
        void shouldDeleteCarWhenValidIdProvided() throws Exception {

            Long carId = 1L;
            doNothing().when(carService).deleteCar(carId);

            mockMvc.perform(delete("/api/cars/{id}", carId))
                    .andExpect(status().isNoContent());

            verify(carService).deleteCar(carId);
        }
    }

    @Nested
    @DisplayName("GET /api/cars")
    class GetAllCarsTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return paginated cars")
        void shouldReturnPaginatedCars() throws Exception {

            Page<CarResponseDto> carPage = new PageImpl<>(List.of(testCarResponseDto));
            when(carService.getAllCars(any())).thenReturn(carPage);

            mockMvc.perform(get("/api/cars")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(1L));

            verify(carService).getAllCars(any());
        }
    }
}
