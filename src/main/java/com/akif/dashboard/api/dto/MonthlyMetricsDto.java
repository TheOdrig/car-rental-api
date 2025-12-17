package com.akif.dashboard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MonthlyMetricsDto(
    BigDecimal totalRevenue,
    int completedRentals,
    int cancelledRentals,
    BigDecimal penaltyRevenue,
    BigDecimal damageCharges,
    BigDecimal averageRentalDurationDays,
    LocalDate startDate,
    LocalDate endDate,
    LocalDateTime generatedAt
) {}

