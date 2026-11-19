package com.akif.damage.api;

import java.math.BigDecimal;
import java.time.Instant;

public record DamageAssessedEvent(
        Long damageReportId,
        Long rentalId,
        Long carId,
        String carLicensePlate,
        String customerEmail,
        DamageSeverity severity,
        BigDecimal repairCostEstimate,
        BigDecimal customerLiability,
        Instant assessedAt) {
}
