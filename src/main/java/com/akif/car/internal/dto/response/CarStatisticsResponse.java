package com.akif.car.internal.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record CarStatisticsResponse(
        long totalCars,
        long activeCars,
        Map<String, Long> statusCounts,
        Map<String, Long> brandCounts,
        BigDecimal averagePrice,
        BigDecimal minPrice,
        BigDecimal maxPrice) {
}
