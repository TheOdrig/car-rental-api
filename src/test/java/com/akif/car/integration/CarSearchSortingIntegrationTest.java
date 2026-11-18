package com.akif.car.integration;

import com.akif.CarRentalApplication;
import com.akif.car.api.CarStatusType;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.shared.enums.CurrencyType;
import com.akif.testsupport.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Car search sorting and page shape")
class CarSearchSortingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarRepository carRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        carRepository.deleteAll();
        save("34CCC111", "Toyota", "Corolla", 2020, "250000", "4.5");
        save("34AAA222", "Honda", "Civic", 2021, "280000", "3.0");
        save("34BBB333", "Toyota", "Yaris", 2020, "250000", "4.5");
        save("34DDD444", "BMW", "320i", 2022, "450000", null);
        save("34EEE555", "Honda", "Jazz", 2019, "180000", "3.0");
    }

    @AfterEach
    void tearDown() {
        carRepository.deleteAll();
    }

    private void save(String plate, String brand, String model, int year, String price, String rating) {
        Car car = new Car();
        car.setLicensePlate(plate);
        car.setVinNumber("VIN" + plate);
        car.setBrand(brand);
        car.setModel(model);
        car.setProductionYear(year);
        car.setPrice(new BigDecimal(price));
        car.setCurrencyType(CurrencyType.TRY);
        car.setDamagePrice(BigDecimal.ZERO);
        car.setCarStatusType(CarStatusType.AVAILABLE);
        car.setRating(rating == null ? null : new BigDecimal(rating));
        carRepository.save(car);
    }

    private List<String> platesSortedBy(String sortBy, String direction) throws Exception {
        String body = mockMvc.perform(get("/api/v1/cars/search")
                        .param("sortBy", sortBy)
                        .param("sortDirection", direction)
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> plates = new ArrayList<>();
        for (JsonNode row : objectMapper.readTree(body).get("content")) {
            plates.add(row.get("licensePlate").asText());
        }
        return plates;
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', value = {
            "id|asc|34CCC111,34AAA222,34BBB333,34DDD444,34EEE555",
            "id|desc|34EEE555,34DDD444,34BBB333,34AAA222,34CCC111",
            "createTime|asc|34CCC111,34AAA222,34BBB333,34DDD444,34EEE555",
            "createTime|desc|34EEE555,34DDD444,34BBB333,34AAA222,34CCC111",
            "licensePlate|asc|34AAA222,34BBB333,34CCC111,34DDD444,34EEE555",
            "licensePlate|desc|34EEE555,34DDD444,34CCC111,34BBB333,34AAA222",
            "brand|asc|34DDD444,34AAA222,34EEE555,34CCC111,34BBB333",
            "brand|desc|34BBB333,34CCC111,34EEE555,34AAA222,34DDD444",
            "model|asc|34DDD444,34AAA222,34CCC111,34EEE555,34BBB333",
            "model|desc|34BBB333,34EEE555,34CCC111,34AAA222,34DDD444",
            "productionYear|asc|34EEE555,34CCC111,34BBB333,34AAA222,34DDD444",
            "productionYear|desc|34DDD444,34AAA222,34BBB333,34CCC111,34EEE555",
            "price|asc|34EEE555,34CCC111,34BBB333,34AAA222,34DDD444",
            "price|desc|34DDD444,34AAA222,34BBB333,34CCC111,34EEE555",
            "rating|asc|34AAA222,34EEE555,34CCC111,34BBB333,34DDD444",
            "rating|desc|34BBB333,34CCC111,34EEE555,34AAA222,34DDD444"
    })
    @DisplayName("sorts by every allowlisted field in both directions with an id tie-breaker")
    void sortsByField(String field, String direction, String expected) throws Exception {
        assertThat(platesSortedBy(field, direction)).containsExactlyElementsOf(Arrays.asList(expected.split(",")));
    }

    @Test
    @DisplayName("tie-breaker keeps pages stable and disjoint")
    void tieBreakerKeepsPagesStable() throws Exception {
        List<String> seen = new ArrayList<>();
        for (int page = 0; page < 5; page++) {
            String body = mockMvc.perform(get("/api/v1/cars/search")
                            .param("sortBy", "brand").param("sortDirection", "asc")
                            .param("page", String.valueOf(page)).param("size", "1"))
                    .andReturn().getResponse().getContentAsString();
            seen.add(objectMapper.readTree(body).get("content").get(0).get("licensePlate").asText());
        }
        assertThat(seen).containsExactly("34DDD444", "34AAA222", "34EEE555", "34CCC111", "34BBB333");
    }

    @Test
    @DisplayName("defaults to newest first when no sort is given")
    void defaultsToNewestFirst() throws Exception {
        String body = mockMvc.perform(get("/api/v1/cars/search").param("size", "50"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> plates = new ArrayList<>();
        for (JsonNode row : objectMapper.readTree(body).get("content")) {
            plates.add(row.get("licensePlate").asText());
        }
        assertThat(plates).containsExactly("34EEE555", "34DDD444", "34BBB333", "34AAA222", "34CCC111");
    }

    @Test
    @DisplayName("rejects a sort field outside the allowlist")
    void rejectsUnknownSortField() throws Exception {
        mockMvc.perform(get("/api/v1/cars/search").param("sortBy", "damagePrice"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("sortBy"));
    }

    @Test
    @DisplayName("GET returns the shared PageResponse shape")
    void getReturnsPageResponse() throws Exception {
        mockMvc.perform(get("/api/v1/cars/search").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.cars").doesNotExist())
                .andExpect(jsonPath("$.currentPage").doesNotExist());
    }

    @Test
    @WithMockUser
    @DisplayName("POST returns the shared PageResponse shape and honours sorting")
    void postReturnsPageResponse() throws Exception {
        mockMvc.perform(post("/api/v1/cars/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sortBy\":\"price\",\"sortDirection\":\"desc\",\"size\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].licensePlate").value("34DDD444"))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.cars").doesNotExist());
    }
}
