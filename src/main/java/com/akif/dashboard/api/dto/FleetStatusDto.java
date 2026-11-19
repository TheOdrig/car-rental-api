package com.akif.dashboard.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record FleetStatusDto(
        int totalCars,
        int availableCars,
        int rentedCars,
        int reservedCars,
        int maintenanceCars,
        int inspectionCars,
        int damagedCars,
        BigDecimal occupancyRate,
        Instant generatedAt) {
}

