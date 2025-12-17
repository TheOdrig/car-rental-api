package com.akif.dashboard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FleetStatusDto(
    int totalCars,
    int availableCars,
    int rentedCars,
    int maintenanceCars,
    int damagedCars,
    BigDecimal occupancyRate,
    LocalDateTime generatedAt
) {}

