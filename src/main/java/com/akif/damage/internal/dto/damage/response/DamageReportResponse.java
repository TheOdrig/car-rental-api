package com.akif.damage.internal.dto.damage.response;

import com.akif.damage.api.DamageCategory;
import com.akif.damage.api.DamageSeverity;
import com.akif.damage.api.DamageStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        LocalDateTime reportedAt,
        LocalDateTime assessedAt,
        List<DamagePhotoDto> photos
){ }
