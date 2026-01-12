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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CarController Integration Tests")
class CarControllerIntegrationTest {

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
                .viewCount(0L)
                .likeCount(0L)
                .build();
        testCar = carRepository.save(testCar);
    }

    @AfterEach
    void tearDown() {
        carRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/cars/{id} - Should return car")
    void shouldReturnCarById() throws Exception {
        mockMvc.perform(get("/api/cars/{id}", testCar.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testCar.getId()))
                .andExpect(jsonPath("$.licensePlate").value("34ABC123"))
                .andExpect(jsonPath("$.brand").value("Toyota"))
                .andExpect(jsonPath("$.model").value("Corolla"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/cars/licensePlate/{licensePlate} - Should return car")
    void shouldReturnCarByLicensePlate() throws Exception {
        mockMvc.perform(get("/api/cars/licensePlate/{licensePlate}", "34ABC123"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.licensePlate").value("34ABC123"))
                .andExpect(jsonPath("$.brand").value("Toyota"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/cars - Should create car")
    void shouldCreateCar() throws Exception {
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
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.licensePlate").value("06XYZ789"))
                .andExpect(jsonPath("$.brand").value("Honda"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/cars/{id} - Should update car")
    void shouldUpdateCar() throws Exception {
        CarRequest updateRequest = CarRequest.builder()
                .licensePlate("34ABC123")
                .vinNumber("1HGBH41JXMN109186")
                .brand("Toyota")
                .model("Corolla Updated")
                .productionYear(2020)
                .price(new BigDecimal("260000"))
                .currencyType(CurrencyType.TRY)
                .damagePrice(BigDecimal.ZERO)
                .carStatusType(CarStatusType.AVAILABLE)
                .isFeatured(true)
                .isTestDriveAvailable(true)
                .build();

        mockMvc.perform(put("/api/cars/{id}", testCar.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value("Corolla Updated"))
                .andExpect(jsonPath("$.price").value(260000));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/cars/{id} - Should delete car")
    void shouldDeleteCar() throws Exception {
        mockMvc.perform(delete("/api/cars/{id}", testCar.getId())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cars/{id}", testCar.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/cars - Should return paginated cars")
    void shouldReturnPaginatedCars() throws Exception {
        mockMvc.perform(get("/api/cars")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].brand").value("Toyota"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/cars/active - Should return active cars")
    void shouldReturnActiveCars() throws Exception {
        mockMvc.perform(get("/api/cars/active")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/cars/{id} - Should return 404 for non-existent car")
    void shouldReturn404ForNonExistentCar() throws Exception {
        mockMvc.perform(get("/api/cars/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/cars/active - Should use default sort (createTime DESC)")
    void shouldUseDefaultSort() throws Exception {
        Car secondCar = Car.builder()
                .licensePlate("06DEF456")
                .vinNumber("2HGBH41JXMN109188")
                .brand("Honda")
                .model("Civic")
                .productionYear(2021)
                .price(new BigDecimal("300000"))
                .currencyType(CurrencyType.TRY)
                .damagePrice(BigDecimal.ZERO)
                .carStatusType(CarStatusType.AVAILABLE)
                .isFeatured(false)
                .isTestDriveAvailable(true)
                .viewCount(0L)
                .likeCount(0L)
                .build();
        carRepository.save(secondCar);

        mockMvc.perform(get("/api/cars/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/cars/active?sort=price,asc - Should sort by price ascending")
    void shouldSortByPriceAscending() throws Exception {
        Car expensiveCar = Car.builder()
                .licensePlate("06DEF456")
                .vinNumber("2HGBH41JXMN109188")
                .brand("Mercedes")
                .model("S-Class")
                .productionYear(2023)
                .price(new BigDecimal("5000000"))
                .currencyType(CurrencyType.TRY)
                .damagePrice(BigDecimal.ZERO)
                .carStatusType(CarStatusType.AVAILABLE)
                .isFeatured(true)
                .isTestDriveAvailable(true)
                .viewCount(0L)
                .likeCount(0L)
                .build();
        carRepository.save(expensiveCar);

        mockMvc.perform(get("/api/cars/active")
                        .param("sort", "price,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].brand").value("Toyota"))
                .andExpect(jsonPath("$.content[1].brand").value("Mercedes"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/cars/active?sort=viewCount,desc - Should sort by view count descending")
    void shouldSortByViewCountDescending() throws Exception {
        Car popularCar = Car.builder()
                .licensePlate("06DEF456")
                .vinNumber("2HGBH41JXMN109188")
                .brand("BMW")
                .model("X5")
                .productionYear(2022)
                .price(new BigDecimal("1500000"))
                .currencyType(CurrencyType.TRY)
                .damagePrice(BigDecimal.ZERO)
                .carStatusType(CarStatusType.AVAILABLE)
                .isFeatured(true)
                .isTestDriveAvailable(true)
                .viewCount(1000L)
                .likeCount(0L)
                .build();
        carRepository.save(popularCar);

        mockMvc.perform(get("/api/cars/active")
                        .param("sort", "viewCount,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].brand").value("BMW"))
                .andExpect(jsonPath("$.content[0].viewCount").value(1000));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/cars/featured - Should return featured cars")
    void shouldReturnFeaturedCars() throws Exception {
        mockMvc.perform(get("/api/cars/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].isFeatured").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/cars/featured - Should exclude non-featured cars")
    void shouldExcludeNonFeaturedCars() throws Exception {
        Car nonFeaturedCar = Car.builder()
                .licensePlate("06DEF456")
                .vinNumber("2HGBH41JXMN109188")
                .brand("Fiat")
                .model("Egea")
                .productionYear(2021)
                .price(new BigDecimal("400000"))
                .currencyType(CurrencyType.TRY)
                .damagePrice(BigDecimal.ZERO)
                .carStatusType(CarStatusType.AVAILABLE)
                .isFeatured(false)
                .isTestDriveAvailable(true)
                .viewCount(0L)
                .likeCount(0L)
                .build();
        carRepository.save(nonFeaturedCar);

        mockMvc.perform(get("/api/cars/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].brand").value("Toyota"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/cars/featured - Should support pagination")
    void shouldSupportPaginationOnFeatured() throws Exception {
        mockMvc.perform(get("/api/cars/featured")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable.pageSize").value(5))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/cars/filter-options - Should return filter options")
    void shouldReturnFilterOptions() throws Exception {
        mockMvc.perform(get("/api/cars/filter-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brands").isArray())
                .andExpect(jsonPath("$.transmissionTypes").isArray())
                .andExpect(jsonPath("$.fuelTypes").isArray())
                .andExpect(jsonPath("$.bodyTypes").isArray());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/cars/filter-options - Should include test car brand")
    void shouldIncludeTestCarBrand() throws Exception {
        mockMvc.perform(get("/api/cars/filter-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brands").isNotEmpty());
    }
}
