package com.akif.car.integration;

import com.akif.CarRentalApplication;
import com.akif.car.domain.Car;
import com.akif.car.api.CarStatusType;
import com.akif.car.internal.repository.CarRepository;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.akif.testsupport.TestcontainersConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
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
        createTestCar("34DEF789", "BMW", "320i", 2022, new BigDecimal("450000"), CarStatusType.MAINTENANCE);
    }

    @AfterEach
    void tearDown() {
        carRepository.deleteAll();
    }

    private void createTestCar(String licensePlate, String brand, String model, int year, BigDecimal price,
            CarStatusType status) {
        Car car = new Car();
        car.setLicensePlate(licensePlate);
        car.setVinNumber("VIN" + licensePlate);
        car.setBrand(brand);
        car.setModel(model);
        car.setProductionYear(year);
        car.setPrice(price);
        car.setCurrencyType(CurrencyType.TRY);
        car.setCarStatusType(status);
        carRepository.save(car);
    }

    @Test
    @DisplayName("Should get cars by status")
    void shouldGetCarsByStatus() throws Exception {
        mockMvc.perform(get("/api/v1/cars/search/status/{status}", "AVAILABLE")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("Should get cars by brand")
    void shouldGetCarsByBrand() throws Exception {
        mockMvc.perform(get("/api/v1/cars/search/brand/{brand}", "Toyota")
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
        mockMvc.perform(get("/api/v1/cars/search/price-range")
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
        mockMvc.perform(get("/api/v1/cars/search/new")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Should search cars by multiple criteria")
    void shouldSearchByMultipleCriteria() throws Exception {
        mockMvc.perform(get("/api/v1/cars/search/criteria")
                .param("brand", "Toyota")
                .param("minPrice", "200000")
                .param("maxPrice", "300000")
                .param("status", "AVAILABLE")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].licensePlate").value("34ABC123"));
    }

    @Test
    @DisplayName("Criteria search matches brand and model partially and case-insensitively")
    void criteriaSearchIsPartialAndCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/v1/cars/search/criteria").param("brand", "toYO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].brand").value("Toyota"));

        mockMvc.perform(get("/api/v1/cars/search/criteria").param("model", "IVI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].model").value("Civic"));

        mockMvc.perform(get("/api/v1/cars/search/criteria").param("searchTerm", "corOL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].licensePlate").value("34ABC123"));
    }

    @Test
    @DisplayName("Criteria search treats LIKE wildcards in the input literally")
    void criteriaSearchEscapesWildcards() throws Exception {
        mockMvc.perform(get("/api/v1/cars/search/criteria").param("brand", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Criteria search keeps results of different criteria apart in the cache")
    void criteriaSearchCacheKeyCoversEveryCriterion() throws Exception {
        mockMvc.perform(get("/api/v1/cars/search/criteria").param("brand", "Toyota").param("minPrice", "100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/cars/search/criteria").param("brand", "Toyota").param("minPrice", "300000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/cars/search/criteria").param("size", "1").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(3));
        mockMvc.perform(get("/api/v1/cars/search/criteria").param("size", "2").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("An unknown status answers 400 with a ProblemDetail")
    void unknownStatusIsAClientError() throws Exception {
        mockMvc.perform(get("/api/v1/cars/search/status/foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CAR_VALIDATION_FAILED"));
    }
}
