package com.akif.car.integration;

import com.akif.CarRentalApplication;
import com.akif.car.api.CarStatusType;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.shared.enums.CurrencyType;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Car currency conversion and the response cache")
class CarCurrencyCacheIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ObjectMapper objectMapper;

    private Car car;

    @BeforeEach
    void setUp() {
        carRepository.deleteAll();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        car = carRepository.save(Car.builder()
                .licensePlate("34ABC123")
                .vinNumber("1HGBH41JXMN109186")
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2020)
                .price(new BigDecimal("250000"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .build());
    }

    @AfterEach
    void tearDown() {
        carRepository.deleteAll();
    }

    @Test
    @DisplayName("a converted request does not leak into the next plain request for one car")
    void singleCarDoesNotLeak() throws Exception {
        mockMvc.perform(get("/api/v1/cars/{id}", car.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayCurrency").doesNotExist());

        mockMvc.perform(get("/api/v1/cars/{id}", car.getId()).param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayCurrency").value("USD"))
                .andExpect(jsonPath("$.convertedPrice").exists());

        mockMvc.perform(get("/api/v1/cars/{id}", car.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayCurrency").doesNotExist())
                .andExpect(jsonPath("$.convertedPrice").doesNotExist())
                .andExpect(jsonPath("$.exchangeRate").doesNotExist());
    }

    @Test
    @DisplayName("a converted request does not leak into the next plain request for lists")
    void pagedListsDoNotLeak() throws Exception {
        for (String path : List.of("/api/v1/cars", "/api/v1/cars/active")) {
            mockMvc.perform(get(path).param("currency", "EUR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].displayCurrency").value("EUR"));

            mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].displayCurrency").doesNotExist())
                    .andExpect(jsonPath("$.content[0].convertedPrice").doesNotExist());
        }
    }

    @Test
    @DisplayName("concurrent requests in different currencies each see only their own currency")
    void concurrentMixedCurrencies() throws Exception {
        String[] currencies = {null, "USD", "EUR", "GBP"};
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < 80; i++) {
                String currency = currencies[i % currencies.length];
                Callable<String> call = () -> {
                    var request = get("/api/v1/cars/{id}", car.getId());
                    if (currency != null) {
                        request.param("currency", currency);
                    }
                    String body = mockMvc.perform(request).andReturn().getResponse().getContentAsString();
                    var node = objectMapper.readTree(body).get("displayCurrency");
                    String seen = node == null ? null : node.asString();
                    return currency + ">" + seen;
                };
                futures.add(pool.submit(call));
            }
            for (Future<String> future : futures) {
                String[] pair = future.get().split(">");
                assertThat(pair[1]).isEqualTo(pair[0]);
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
