package com.akif.dto.damage.response;

import com.akif.enums.DamageCategory;
import com.akif.enums.DamageSeverity;
import com.akif.enums.DamageStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record DamageReportResponseDto(
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
        List<DamagePhotoDto> photos
){ }
