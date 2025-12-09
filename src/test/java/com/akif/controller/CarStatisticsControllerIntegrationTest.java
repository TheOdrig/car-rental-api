package com.akif.controller;

import com.akif.shared.enums.CarStatusType;
import com.akif.shared.enums.CurrencyType;
import com.akif.model.Car;
import com.akif.repository.CarRepository;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.*;
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
@DisplayName("CarStatisticsController Integration Tests")
public class CarStatisticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarRepository carRepository;

    @BeforeEach
    void setUp() {
        carRepository.deleteAll();

        createTestCar("34ABC123", "Toyota", "Corolla", new BigDecimal("250000"), CarStatusType.AVAILABLE);
        createTestCar("34XYZ456", "Toyota", "Camry", new BigDecimal("350000"), CarStatusType.AVAILABLE);
        createTestCar("34DEF789", "Honda", "Civic", new BigDecimal("280000"), CarStatusType.SOLD);
        createTestCar("34GHI012", "BMW", "320i", new BigDecimal("450000"), CarStatusType.RESERVED);
    }

    @AfterEach
    void tearDown() {
        carRepository.deleteAll();
    }

    private void createTestCar(String licensePlate, String brand, String model, BigDecimal price, CarStatusType status) {
        Car car = new Car();
        car.setLicensePlate(licensePlate);
        car.setVinNumber("VIN" + licensePlate);
        car.setBrand(brand);
        car.setModel(model);
        car.setProductionYear(2020);
        car.setPrice(price);
        car.setCurrencyType(CurrencyType.TRY);
        car.setDamagePrice(BigDecimal.ZERO);
        car.setCarStatusType(status);
        car.setIsFeatured(false);
        car.setIsTestDriveAvailable(true);
        carRepository.save(car);
    }

    @Test
    @DisplayName("Should get car statistics")
    void shouldGetCarStatistics() throws Exception {
        mockMvc.perform(get("/api/cars/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCars").value(4))
                .andExpect(jsonPath("$.activeCars").value(4))
                .andExpect(jsonPath("$.averagePrice").exists())
                .andExpect(jsonPath("$.minPrice").exists())
                .andExpect(jsonPath("$.maxPrice").exists());
    }

    @Test
    @DisplayName("Should get cars count by status")
    void shouldGetCarsCountByStatus() throws Exception {
        mockMvc.perform(get("/api/cars/statistics/status-counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.AVAILABLE").value(2))
                .andExpect(jsonPath("$.SOLD").value(1))
                .andExpect(jsonPath("$.RESERVED").value(1));
    }

    @Test
    @DisplayName("Should get cars count by brand")
    void shouldGetCarsCountByBrand() throws Exception {
        mockMvc.perform(get("/api/cars/statistics/brand-counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Toyota").value(2))
                .andExpect(jsonPath("$.Honda").value(1))
                .andExpect(jsonPath("$.BMW").value(1));
    }

    @Test
    @DisplayName("Should get average prices by brand")
    void shouldGetAveragePricesByBrand() throws Exception {
        mockMvc.perform(get("/api/cars/statistics/average-prices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Toyota").exists())
                .andExpect(jsonPath("$.Honda").exists())
                .andExpect(jsonPath("$.BMW").exists());
    }

    @Test
    @DisplayName("Should get total car count")
    void shouldGetTotalCarCount() throws Exception {
        mockMvc.perform(get("/api/cars/statistics/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4));
    }

}
