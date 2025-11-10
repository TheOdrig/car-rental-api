package com.akif.controller;

import com.akif.dto.request.CarPriceUpdateRequestDto;
import com.akif.dto.request.CarStatusUpdateRequestDto;
import com.akif.enums.CarStatusType;
import com.akif.enums.CurrencyType;
import com.akif.model.Car;
import com.akif.repository.CarRepository;
import com.akif.starter.CarGalleryProjectApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CarBusinessController Integration Tests")
public class CarBusinessControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CarRepository carRepository;

    private Long testCarId;

    @BeforeEach
    void setUp() {
        carRepository.deleteAll();
        
        Car car = new Car();
        car.setLicensePlate("34TEST123");
        car.setVinNumber("TEST123456789");
        car.setBrand("Toyota");
        car.setModel("Corolla");
        car.setProductionYear(2020);
        car.setPrice(new BigDecimal("250000"));
        car.setCurrencyType(CurrencyType.TRY);
        car.setDamagePrice(BigDecimal.ZERO);
        car.setCarStatusType(CarStatusType.AVAILABLE);
        car.setIsFeatured(true);
        car.setIsTestDriveAvailable(true);
        
        testCarId = carRepository.save(car).getId();
    }

    @AfterEach
    void tearDown() {
        carRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should sell car successfully")
    void shouldSellCar() throws Exception {
        mockMvc.perform(post("/api/cars/business/{id}/sell", testCarId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCarId))
                .andExpect(jsonPath("$.carStatusType").value("Sold"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should reserve car successfully")
    void shouldReserveCar() throws Exception {
        mockMvc.perform(post("/api/cars/business/{id}/reserve", testCarId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCarId))
                .andExpect(jsonPath("$.carStatusType").value("Reserved"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should update car status successfully")
    void shouldUpdateCarStatus() throws Exception {
        CarStatusUpdateRequestDto request = CarStatusUpdateRequestDto.builder()
                .carStatusType(CarStatusType.MAINTENANCE)
                .reason("Regular maintenance")
                .build();

        mockMvc.perform(patch("/api/cars/business/{id}/status", testCarId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCarId))
                .andExpect(jsonPath("$.carStatusType").value("Maintenance"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should update car price successfully")
    void shouldUpdateCarPrice() throws Exception {
        CarPriceUpdateRequestDto request = CarPriceUpdateRequestDto.builder()
                .price(new BigDecimal("240000"))
                .currencyType(CurrencyType.TRY)
                .damagePrice(BigDecimal.ZERO)
                .build();

        mockMvc.perform(patch("/api/cars/business/{id}/price", testCarId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCarId))
                .andExpect(jsonPath("$.price").value(240000));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should increment view count successfully")
    void shouldIncrementViewCount() throws Exception {
        mockMvc.perform(post("/api/cars/business/{id}/view", testCarId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should check if car can be sold")
    void shouldCheckCanBeSold() throws Exception {
        mockMvc.perform(get("/api/cars/business/{id}/can-sell", testCarId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canBeSold").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should check if car can be reserved")
    void shouldCheckCanBeReserved() throws Exception {
        mockMvc.perform(get("/api/cars/business/{id}/can-reserve", testCarId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canBeReserved").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should deny access to business endpoints for non-admin users")
    void shouldDenyAccessForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/cars/business/{id}/sell", testCarId))
                .andExpect(status().isForbidden());
    }
}
