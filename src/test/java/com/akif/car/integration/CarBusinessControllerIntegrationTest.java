package com.akif.car.integration;

import com.akif.CarRentalApplication;
import com.akif.car.domain.Car;
import com.akif.car.api.CarStatusType;
import com.akif.car.internal.dto.pricing.CarPriceUpdateRequest;
import com.akif.car.internal.dto.request.CarStatusUpdateRequest;
import com.akif.car.internal.repository.CarRepository;
import com.akif.shared.enums.CurrencyType;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.akif.testsupport.TestcontainersConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CarBusinessController Integration Tests")
class CarBusinessControllerIntegrationTest {

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
        car.setCarStatusType(CarStatusType.AVAILABLE);
        
        testCarId = carRepository.save(car).getId();
    }

    @AfterEach
    void tearDown() {
        carRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should update car status successfully")
    void shouldUpdateCarStatus() throws Exception {
        CarStatusUpdateRequest request = new CarStatusUpdateRequest(
                CarStatusType.MAINTENANCE,
                "Regular maintenance",
                null
        );

        mockMvc.perform(patch("/api/v1/cars/business/{id}/status", testCarId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCarId))
                .andExpect(jsonPath("$.carStatusType").value("MAINTENANCE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should update car price successfully")
    void shouldUpdateCarPrice() throws Exception {
        CarPriceUpdateRequest request = new CarPriceUpdateRequest(
                new BigDecimal("240000"),
                CurrencyType.TRY,
                "Price update",
                null
        );

        mockMvc.perform(patch("/api/v1/cars/business/{id}/price", testCarId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCarId))
                .andExpect(jsonPath("$.price").value(240000));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should reject a transition to the status the car already has")
    void shouldRejectSameStatusTransition() throws Exception {
        CarStatusUpdateRequest request = new CarStatusUpdateRequest(CarStatusType.AVAILABLE, null, null);

        mockMvc.perform(patch("/api/v1/cars/business/{id}/status", testCarId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should move a car through maintenance and back to available")
    void shouldMoveThroughMaintenance() throws Exception {
        mockMvc.perform(post("/api/v1/cars/business/{id}/maintenance", testCarId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carStatusType").value("MAINTENANCE"));
        mockMvc.perform(post("/api/v1/cars/business/{id}/available", testCarId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carStatusType").value("AVAILABLE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should no longer expose the car-level reservation endpoints")
    void reservationEndpointsAreGone() throws Exception {
        mockMvc.perform(post("/api/v1/cars/business/{id}/reserve", testCarId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/cars/business/{id}/can-reserve", testCarId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should deny access to business endpoints for non-admin users")
    void shouldDenyAccessForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/cars/business/{id}/maintenance", testCarId))
                .andExpect(status().isForbidden());
    }
}
