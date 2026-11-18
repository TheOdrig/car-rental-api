package com.akif.damage.api;

import com.akif.damage.domain.enums.DamageCategory;
import com.akif.damage.api.DamageSeverity;
import com.akif.damage.domain.enums.DamageStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    LocalDateTime reportedAt,
    LocalDateTime assessedAt,
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
            LocalDateTime reportedAt,
            LocalDateTime assessedAt) {
        this(id, rentalId, carId, carLicensePlate, customerName, description, damageLocation, severity, category,
                status, repairCostEstimate, customerLiability, insuranceCoverage, reportedAt, assessedAt, null);
    }

    public boolean isPending() {
        return status != null && !status.isTerminal();
    }
}
