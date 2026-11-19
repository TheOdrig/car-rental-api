package com.akif.damage.api;

import com.akif.car.api.CarSummaryResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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
    CurrencyType currency,
    CarSummaryResponse carSummary,
    String assessmentNotes,
    BigDecimal insuranceDeductible,
    List<DamagePhotoDto> photos
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
                status, repairCostEstimate, customerLiability, insuranceCoverage, reportedAt, assessedAt, null, null, null, null, null);
    }

    public DamageReportDto withPhotos(List<DamagePhotoDto> photos) {
        return new DamageReportDto(id, rentalId, carId, carLicensePlate, customerName, description, damageLocation,
                severity, category, status, repairCostEstimate, customerLiability, insuranceCoverage, reportedAt,
                assessedAt, currency, carSummary, assessmentNotes, insuranceDeductible, photos);
    }

    public boolean isPending() {
        return status != null && !status.isTerminal();
    }
}
