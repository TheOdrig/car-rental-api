package com.akif.damage.internal.dto.damage.response;

import com.akif.car.api.CarSummaryResponse;
import com.akif.damage.api.DamageCategory;
import com.akif.damage.api.DamageSeverity;
import com.akif.damage.api.DamageStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DamageReportResponse(
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
        CurrencyType currency,
        Boolean insuranceCoverage,
        Instant reportedAt,
        Instant assessedAt,
        String disputeReason,
        String disputeComments,
        Instant disputedAt,
        BigDecimal originalLiability,
        BigDecimal refundAmount,
        String resolutionNotes,
        Instant resolvedAt,
        List<DamagePhotoDto> photos,
        CarSummaryResponse carSummary
){ }
