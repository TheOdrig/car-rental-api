package com.akif.damage.internal.dto.damage.response;

import com.akif.damage.api.DamageSeverity;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DamageAssessmentResponse(
    
    Long damageId,
    DamageSeverity severity,
    BigDecimal repairCostEstimate,
    BigDecimal customerLiability,
    Boolean insuranceCoverage,
    BigDecimal insuranceDeductible,
    String assessmentNotes,
    LocalDateTime assessedAt,
    Long assessedBy,
    String carStatusUpdated
) {}
