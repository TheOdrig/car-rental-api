package com.akif.dashboard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

public record MonthlyMetricsDto(
    List<RevenueBreakdownDto> revenueByCurrency,
    int completedRentals,
    int cancelledRentals,
    BigDecimal averageRentalDurationDays,
    LocalDate startDate,
    LocalDate endDate,
    Instant generatedAt
) {}
