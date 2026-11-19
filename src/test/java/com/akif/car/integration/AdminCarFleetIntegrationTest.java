package com.akif.car.integration;

import com.akif.CarRentalApplication;
import com.akif.car.api.CarStatusType;
import com.akif.car.domain.Car;
import com.akif.car.internal.dto.request.CarRequest;
import com.akif.car.internal.repository.CarRepository;
import com.akif.shared.enums.CurrencyType;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Admin fleet search and car field rules")
class AdminCarFleetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        carRepository.deleteAll();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        for (int i = 0; i < 30; i++) {
            carRepository.save(Car.builder()
                    .licensePlate(String.format("34ABC%03d", i))
                    .brand(i % 3 == 0 ? "Toyota" : "Honda")
                    .model("Model" + i)
                    .productionYear(2020)
                    .price(new BigDecimal(100 + i))
                    .currencyType(CurrencyType.TRY)
                    .carStatusType(i % 5 == 0 ? CarStatusType.MAINTENANCE : CarStatusType.AVAILABLE)
                    .vinNumber(String.format("1HGBH41JXMN%06d", i))
                    .build());
        }
    }

    @AfterEach
    void tearDown() {
        carRepository.deleteAll();
    }

    private CarRequest.CarRequestBuilder base(String plate) {
        return CarRequest.builder()
                .licensePlate(plate)
                .brand("Kia")
                .model("Rio")
                .productionYear(2021)
                .price(new BigDecimal("200"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("the admin list pages beyond the first 25 cars and reports the total")
    void adminListPages() throws Exception {
        mockMvc.perform(get("/api/v1/admin/cars").param("size", "25").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(30))
                .andExpect(jsonPath("$.content", hasSize(5)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("the admin list filters by status, brand and free text on the server")
    void adminListFilters() throws Exception {
        mockMvc.perform(get("/api/v1/admin/cars").param("status", "MAINTENANCE"))
                .andExpect(jsonPath("$.totalElements").value(6));
        mockMvc.perform(get("/api/v1/admin/cars").param("brand", "toyota"))
                .andExpect(jsonPath("$.totalElements").value(10));
        mockMvc.perform(get("/api/v1/admin/cars").param("search", "model2"))
                .andExpect(jsonPath("$.totalElements").value(11));
        mockMvc.perform(get("/api/v1/admin/cars").param("sort", "price,desc").param("size", "1"))
                .andExpect(jsonPath("$.content[0].price").value(129))
                .andExpect(jsonPath("$.content[0].vinNumber").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("an unknown admin sort field answers 400")
    void adminListRejectsUnknownSort() throws Exception {
        mockMvc.perform(get("/api/v1/admin/cars").param("sort", "vinNumber"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("customers cannot use the admin list")
    void adminListIsForbiddenForCustomers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/cars")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("public car responses do not expose the VIN")
    void publicResponsesHideVin() throws Exception {
        mockMvc.perform(get("/api/v1/cars/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].vinNumber").doesNotExist());
        mockMvc.perform(get("/api/v1/cars/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].vinNumber").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("several cars can be created without a VIN or an image")
    void carsWithoutVinCoexist() throws Exception {
        for (String plate : new String[]{"06AAA111", "06AAA112"}) {
            mockMvc.perform(post("/api/v1/cars").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(base(plate).build())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.vinNumber").doesNotExist())
                    .andExpect(jsonPath("$.imageUrl").doesNotExist());
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("updating without a VIN keeps the stored one")
    void updateWithoutVinKeepsStoredVin() throws Exception {
        Car stored = carRepository.findAll().get(0);
        CarRequest update = base(stored.getLicensePlate()).model("Changed").build();
        mockMvc.perform(put("/api/v1/cars/{id}", stored.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vinNumber").value(stored.getVinNumber()))
                .andExpect(jsonPath("$.model").value("Changed"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("fuel, transmission and body type are stored in one canonical case")
    void specsAreCanonical() throws Exception {
        for (String plate : new String[]{"06BBB111", "06BBB112"}) {
            String variant = plate.endsWith("1") ? "manual" : "MANUAL";
            mockMvc.perform(post("/api/v1/cars").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(base(plate).transmissionType(variant)
                                    .fuelType("diesel").bodyType("suv").build())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.transmissionType").value("Manual"))
                    .andExpect(jsonPath("$.fuelType").value("Diesel"))
                    .andExpect(jsonPath("$.bodyType").value("SUV"));
        }
        mockMvc.perform(get("/api/v1/cars/filter-options"))
                .andExpect(jsonPath("$.transmissionTypes", hasSize(1)));
    }
}
