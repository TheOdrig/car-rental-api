package com.akif.car.internal.service;

import com.akif.car.api.CarStatusType;
import com.akif.car.api.FilterOptionsResponse;
import com.akif.car.internal.dto.response.CarStatisticsResponse;
import com.akif.car.internal.repository.CarRepository;
import com.akif.shared.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CarStatisticsService {

    private final CarRepository carRepository;

    @Cacheable(value = CacheConfig.CAR_STATISTICS_CACHE, key = "'all'")
    public CarStatisticsResponse getCarStatistics() {
        log.debug("Getting car statistics");

        CarStatisticsResponse statistics = new CarStatisticsResponse(
                carRepository.count(),
                carRepository.countByIsDeletedFalse(),
                getCarsCountByStatus(),
                getCarsCountByBrand(),
                carRepository.getAveragePrice(),
                carRepository.getMinPrice(),
                carRepository.getMaxPrice());

        log.info("Successfully retrieved car statistics");
        return statistics;
    }

    @Cacheable(value = CacheConfig.CAR_STATUS_COUNTS_CACHE, key = "'all'")
    public Map<String, Long> getCarsCountByStatus() {
        log.debug("Getting cars count by status");

        Map<String, Long> statusCounts = new HashMap<>();
        for (CarStatusType status : CarStatusType.values()) {
            statusCounts.put(status.name(), carRepository.countByCarStatusTypeAndIsDeletedFalse(status));
        }

        log.info("Successfully retrieved cars count by status");
        return statusCounts;
    }

    @Cacheable(value = CacheConfig.CAR_BRAND_COUNTS_CACHE, key = "'all'")
    public Map<String, Long> getCarsCountByBrand() {
        log.debug("Getting cars count by brand");

        List<Object[]> results = carRepository.getCarsCountByBrand();
        Map<String, Long> brandCounts = new HashMap<>();

        for (Object[] result : results) {
            brandCounts.put((String) result[0], (Long) result[1]);
        }

        log.info("Successfully retrieved cars count by brand: {} brands found", brandCounts.size());
        return brandCounts;
    }

    @Cacheable(value = CacheConfig.CAR_AVERAGE_PRICES_CACHE, key = "'all'")
    public Map<String, BigDecimal> getAveragePriceByBrand() {
        log.debug("Getting average price by brand");

        List<Object[]> results = carRepository.getAveragePriceByBrand();
        Map<String, BigDecimal> averagePrices = new HashMap<>();

        for (Object[] result : results) {
            averagePrices.put((String) result[0], (BigDecimal) result[1]);
        }

        log.info("Successfully retrieved average price by brand: {} brands found", averagePrices.size());
        return averagePrices;
    }

    @Cacheable(value = CacheConfig.FILTER_OPTIONS_CACHE, key = "'all'")
    public FilterOptionsResponse getFilterOptions() {
        log.debug("Fetching filter options from database");

        FilterOptionsResponse response = new FilterOptionsResponse(
                carRepository.findDistinctBrands(),
                carRepository.findDistinctTransmissionTypes(),
                carRepository.findDistinctFuelTypes(),
                carRepository.findDistinctBodyTypes());

        log.info(
                "Successfully retrieved filter options: {} brands, {} transmission types, {} fuel types, {} body types",
                response.brands().size(),
                response.transmissionTypes().size(),
                response.fuelTypes().size(),
                response.bodyTypes().size());

        return response;
    }
}
