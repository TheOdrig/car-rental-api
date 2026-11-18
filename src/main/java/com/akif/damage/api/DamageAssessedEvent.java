package com.akif.damage.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DamageAssessedEvent(
        Long damageReportId,
        Long rentalId,
        Long carId,
        String carLicensePlate,
        String customerEmail,
        DamageSeverity severity,
        BigDecimal repairCostEstimate,
        BigDecimal customerLiability,
        LocalDateTime assessedAt) {
}
