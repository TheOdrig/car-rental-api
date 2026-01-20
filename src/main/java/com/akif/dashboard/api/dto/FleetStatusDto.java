package com.akif.dashboard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FleetStatusDto(
        int totalCars,
        int availableCars,
        int rentedCars,
        int reservedCars,
        int maintenanceCars,
        int inspectionCars,
        int damagedCars,
        int soldCars,
        BigDecimal occupancyRate,
        LocalDateTime generatedAt) {
}

