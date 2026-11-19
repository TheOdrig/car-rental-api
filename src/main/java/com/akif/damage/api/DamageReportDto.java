package com.akif.damage.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DamageReportDto(
    Long id,
    Long rentalId,
    Long carId,
    String carLicensePlate,
    String customerName,
    String description,
    String damageLocation,
    DamageSeverity severity,
    DamageCategory category,
    DamageStatus status,
    BigDecimal repairCostEstimate,
    BigDecimal customerLiability,
    Boolean insuranceCoverage,
    Instant reportedAt,
    Instant assessedAt,
    CurrencyType currency
) {

    public DamageReportDto(
            Long id,
            Long rentalId,
            Long carId,
            String carLicensePlate,
            String customerName,
            String description,
            String damageLocation,
            DamageSeverity severity,
            DamageCategory category,
            DamageStatus status,
            BigDecimal repairCostEstimate,
            BigDecimal customerLiability,
            Boolean insuranceCoverage,
            Instant reportedAt,
            Instant assessedAt) {
        this(id, rentalId, carId, carLicensePlate, customerName, description, damageLocation, severity, category,
                status, repairCostEstimate, customerLiability, insuranceCoverage, reportedAt, assessedAt, null);
    }

    public boolean isPending() {
        return status != null && !status.isTerminal();
    }
}
