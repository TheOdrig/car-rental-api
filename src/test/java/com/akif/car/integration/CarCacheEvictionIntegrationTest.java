package com.akif.car.integration;

import com.akif.CarRentalApplication;
import com.akif.car.internal.service.CarCommandService;
import com.akif.car.internal.service.CarStatisticsService;
import com.akif.car.api.CarStatusType;
import com.akif.car.api.FilterOptionsResponse;
import com.akif.car.internal.dto.pricing.CarPriceUpdateRequest;
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
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Car cache eviction Integration Tests")
class CarCacheEvictionIntegrationTest {

    @Autowired
    private CarCommandService commandService;

    @Autowired
    private CarStatisticsService statisticsService;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        carRepository.deleteAll();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @AfterEach
    void tearDown() {
        carRepository.deleteAll();
    }

    @Test
    @DisplayName("Creating a car refreshes cached filter options, counts and statistics")
    void shouldRefreshEveryCarCacheWhenCarIsCreated() {
        assertThat(statisticsService.getFilterOptions().brands()).isEmpty();
        assertThat(statisticsService.getCarsCountByBrand()).isEmpty();
        assertThat(statisticsService.getCarsCountByStatus().get(CarStatusType.AVAILABLE.name())).isZero();
        assertThat(statisticsService.getAveragePriceByBrand()).isEmpty();

        commandService.createCar(carRequest("34ABC123", "VIN00000000000001", "Toyota"));

        FilterOptionsResponse filterOptions = statisticsService.getFilterOptions();
        assertThat(filterOptions.brands()).containsExactly("Toyota");
        assertThat(statisticsService.getCarsCountByBrand()).containsEntry("Toyota", 1L);
        assertThat(statisticsService.getCarsCountByStatus()).containsEntry(CarStatusType.AVAILABLE.name(), 1L);
        assertThat(statisticsService.getAveragePriceByBrand()).containsKey("Toyota");
        assertThat(statisticsService.getCarStatistics().totalCars()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Updating a price refreshes the cached average price by brand")
    void shouldRefreshAveragePriceWhenPriceIsUpdated() {
        Long carId = commandService.createCar(carRequest("34ABC123", "VIN00000000000001", "Toyota")).getId();
        BigDecimal before = statisticsService.getAveragePriceByBrand().get("Toyota");

        commandService.updateCarPrice(carId,
                new CarPriceUpdateRequest(new BigDecimal("999.00"), CurrencyType.TRY, "reprice", null));

        BigDecimal after = statisticsService.getAveragePriceByBrand().get("Toyota");
        assertThat(after).isNotEqualByComparingTo(before);
        assertThat(after).isEqualByComparingTo("999.00");
    }

    private CarRequest carRequest(String plate, String vin, String brand) {
        return CarRequest.builder()
                .licensePlate(plate)
                .vinNumber(vin)
                .brand(brand)
                .model("Corolla")
                .productionYear(2020)
                .price(new BigDecimal("250.00"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .build();
    }
}
