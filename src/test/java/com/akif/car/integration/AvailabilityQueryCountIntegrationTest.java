package com.akif.car.integration;

import com.akif.CarRentalApplication;
import com.akif.car.api.CarStatusType;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.shared.enums.CurrencyType;
import com.akif.testsupport.TestcontainersConfiguration;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class, properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Availability search statement counts")
class AvailabilityQueryCountIntegrationTest {

    private static final long MAX_STATEMENTS = 12;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        carRepository.deleteAll();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        for (int i = 0; i < 40; i++) {
            carRepository.save(Car.builder()
                    .licensePlate(String.format("34ABC%03d", i))
                    .brand("Toyota")
                    .model("Model" + i)
                    .productionYear(2020)
                    .bodyType("Sedan")
                    .price(new BigDecimal(1000 + i))
                    .currencyType(CurrencyType.TRY)
                    .carStatusType(CarStatusType.AVAILABLE)
                    .build());
        }
    }

    @AfterEach
    void tearDown() {
        carRepository.deleteAll();
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private long statementsFor(int size, String currency) throws Exception {
        LocalDate start = LocalDate.now().plusDays(5);
        String body = "{\"startDate\":\"" + start + "\",\"endDate\":\"" + start.plusDays(3) + "\",\"size\":" + size
                + (currency != null ? ",\"targetCurrency\":\"" + currency + "\"" : "") + "}";
        statistics().clear();
        mockMvc.perform(post("/api/v1/cars/availability/search").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cars.length()").value(size));
        return statistics().getPrepareStatementCount();
    }

    @Test
    @DisplayName("public availability search uses a bounded number of statements regardless of page size")
    void searchStatementCountIsIndependentOfResultSize() throws Exception {
        long small = statementsFor(2, null);
        long large = statementsFor(40, null);
        long converted = statementsFor(40, "USD");

        assertThat(small).isLessThanOrEqualTo(MAX_STATEMENTS);
        assertThat(large).isEqualTo(small);
        assertThat(converted).isLessThanOrEqualTo(MAX_STATEMENTS);
    }

    @Test
    @DisplayName("similar cars use a bounded number of statements")
    void similarCarsStatementCountIsBounded() throws Exception {
        Long referenceId = carRepository.findAll().get(0).getId();
        LocalDate start = LocalDate.now().plusDays(5);
        statistics().clear();
        mockMvc.perform(get("/api/v1/cars/{id}/similar", referenceId)
                        .param("startDate", start.toString())
                        .param("endDate", start.plusDays(3).toString())
                        .param("limit", "10"))
                .andExpect(status().isOk());
        assertThat(statistics().getPrepareStatementCount()).isLessThanOrEqualTo(MAX_STATEMENTS);
    }
}
