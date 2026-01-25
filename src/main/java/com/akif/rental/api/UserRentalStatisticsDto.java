package com.akif.rental.api;

import java.math.BigDecimal;

public record UserRentalStatisticsDto(
        int totalRentals,
        int completedRentals,
        int cancelledRentals,
        int activeRentals,
        BigDecimal totalSpent,
        double averageRentalDuration,
        int lateReturns) {
}
