package com.akif.car.integration;

import com.akif.car.domain.Car;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.car.internal.repository.CarRepository;
import com.akif.shared.enums.CurrencyType;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CarSearchController Integration Tests")
class CarSearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarRepository carRepository;

    @BeforeEach
    void setUp() {
        carRepository.deleteAll();

        createTestCar("34ABC123", "Toyota", "Corolla", 2020, new BigDecimal("250000"), CarStatusType.AVAILABLE);
        createTestCar("34XYZ456", "Honda", "Civic", 2021, new BigDecimal("280000"), CarStatusType.AVAILABLE);
        createTestCar("34DEF789", "BMW", "320i", 2022, new BigDecimal("450000"), CarStatusType.RESERVED);
    }

    @AfterEach
    void tearDown() {
        carRepository.deleteAll();
    }

    private void createTestCar(String licensePlate, String brand, String model, int year, BigDecimal price, CarStatusType status) {
        Car car = new Car();
        car.setLicensePlate(licensePlate);
        car.setVinNumber("VIN" + licensePlate);
        car.setBrand(brand);
        car.setModel(model);
        car.setProductionYear(year);
        car.setPrice(price);
        car.setCurrencyType(CurrencyType.TRY);
        car.setDamagePrice(BigDecimal.ZERO);
        car.setCarStatusType(status);
        car.setIsFeatured(false);
        car.setIsTestDriveAvailable(true);
        carRepository.save(car);
    }

    @Test
    @DisplayName("Should get cars by status")
    void shouldGetCarsByStatus() throws Exception {
        mockMvc.perform(get("/api/cars/search/status/{status}", "AVAILABLE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("Should get cars by brand")
    void shouldGetCarsByBrand() throws Exception {
        mockMvc.perform(get("/api/cars/search/brand/{brand}", "Toyota")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].brand").value("Toyota"));
    }

    @Test
    @DisplayName("Should get cars by price range")
    void shouldGetCarsByPriceRange() throws Exception {
        mockMvc.perform(get("/api/cars/search/price-range")
                        .param("minPrice", "200000")
                        .param("maxPrice", "300000")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("Should get new cars")
    void shouldGetNewCars() throws Exception {
        mockMvc.perform(get("/api/cars/search/new")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Should get featured cars")
    void shouldGetFeaturedCars() throws Exception {
        mockMvc.perform(get("/api/cars/search/featured")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Should search cars by multiple criteria")
    void shouldSearchByMultipleCriteria() throws Exception {
        mockMvc.perform(get("/api/cars/search/criteria")
                        .param("brand", "Toyota")
                        .param("minPrice", "200000")
                        .param("maxPrice", "300000")
                        .param("status", "AVAILABLE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
