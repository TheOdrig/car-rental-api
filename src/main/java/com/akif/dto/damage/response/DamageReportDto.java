package com.akif.dto.damage.response;

import com.akif.shared.enums.DamageCategory;
import com.akif.shared.enums.DamageSeverity;
import com.akif.shared.enums.DamageStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

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
    DamageSeverity severity,
    DamageCategory category,
    DamageStatus status,
    
    BigDecimal repairCostEstimate,
    BigDecimal customerLiability,
    LocalDateTime reportedAt
) {}
