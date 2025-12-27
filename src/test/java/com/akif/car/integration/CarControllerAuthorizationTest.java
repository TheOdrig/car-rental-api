package com.akif.car.integration;

import com.akif.car.domain.Car;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.car.internal.dto.request.CarRequest;
import com.akif.car.internal.repository.CarRepository;
import com.akif.shared.enums.CurrencyType;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CarController Authorization Integration Tests")
class CarControllerAuthorizationTest {

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
                .isDeleted(false)
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
    @DisplayName("Admin Authorization - Should Allow ADMIN Role")
    class AdminShouldBeAllowed {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /api/cars - Admin should be allowed to create car")
        void adminShouldCreateCar() throws Exception {
            CarRequest newCar = CarRequest.builder()
                    .licensePlate("06XYZ789")
                    .vinNumber("2HGBH41JXMN109187")
                    .brand("Honda")
                    .model("Civic")
                    .productionYear(2021)
                    .price(new BigDecimal("300000"))
                    .currencyType(CurrencyType.TRY)
                    .damagePrice(BigDecimal.ZERO)
                    .carStatusType(CarStatusType.AVAILABLE)
                    .isFeatured(false)
                    .isTestDriveAvailable(true)
                    .build();

            mockMvc.perform(post("/api/cars")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newCar)))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("DELETE /api/cars/{id}/soft - Admin should be allowed to soft delete")
        void adminShouldSoftDeleteCar() throws Exception {
            mockMvc.perform(delete("/api/cars/{id}/soft", testCar.getId())
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /api/cars/{id}/restore - Admin should be allowed to restore car")
        void adminShouldRestoreCar() throws Exception {
            // First soft delete
            testCar.softDelete();
            carRepository.save(testCar);

            mockMvc.perform(post("/api/cars/{id}/restore", testCar.getId())
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testCar.getId()));
        }
    }

    @Nested
    @DisplayName("User Authorization - Should Deny USER Role on Admin Operations")
    class UserShouldBeDenied {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("POST /api/cars - User should be denied to create car (403)")
        void userShouldBeDeniedCreateCar() throws Exception {
            CarRequest newCar = CarRequest.builder()
                    .licensePlate("06XYZ789")
                    .vinNumber("2HGBH41JXMN109187")
                    .brand("Honda")
                    .model("Civic")
                    .productionYear(2021)
                    .price(new BigDecimal("300000"))
                    .currencyType(CurrencyType.TRY)
                    .damagePrice(BigDecimal.ZERO)
                    .carStatusType(CarStatusType.AVAILABLE)
                    .build();

            mockMvc.perform(post("/api/cars")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newCar)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("PUT /api/cars/{id} - User should be denied to update car (403)")
        void userShouldBeDeniedUpdateCar() throws Exception {
            CarRequest updateRequest = CarRequest.builder()
                    .licensePlate("34ABC123")
                    .vinNumber("1HGBH41JXMN109186")
                    .brand("Toyota")
                    .model("Corolla Updated")
                    .productionYear(2020)
                    .price(new BigDecimal("260000"))
                    .currencyType(CurrencyType.TRY)
                    .carStatusType(CarStatusType.AVAILABLE)
                    .build();

            mockMvc.perform(put("/api/cars/{id}", testCar.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("DELETE /api/cars/{id} - User should be denied to delete car (403)")
        void userShouldBeDeniedDeleteCar() throws Exception {
            mockMvc.perform(delete("/api/cars/{id}", testCar.getId())
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("DELETE /api/cars/{id}/soft - User should be denied to soft delete car (403)")
        void userShouldBeDeniedSoftDeleteCar() throws Exception {
            mockMvc.perform(delete("/api/cars/{id}/soft", testCar.getId())
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("POST /api/cars/{id}/restore - User should be denied to restore car (403)")
        void userShouldBeDeniedRestoreCar() throws Exception {
            mockMvc.perform(post("/api/cars/{id}/restore", testCar.getId())
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Read Operations - Should Allow All Authenticated Users")
    class ReadOperationsShouldBeAllowed {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/cars/{id} - User should be allowed to read car")
        void userShouldReadCar() throws Exception {
            mockMvc.perform(get("/api/cars/{id}", testCar.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testCar.getId()));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/cars/active - User should be allowed to list active cars")
        void userShouldListActiveCars() throws Exception {
            mockMvc.perform(get("/api/cars/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }
}
