package com.akif.damage.internal.dto.damage.response;

import com.akif.damage.api.DamageSeverity;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DamageAssessmentResponse(
    
    Long damageId,
    DamageSeverity severity,
    BigDecimal repairCostEstimate,
    BigDecimal customerLiability,
    CurrencyType currency,
    Boolean insuranceCoverage,
    BigDecimal insuranceDeductible,
    String assessmentNotes,
    Instant assessedAt,
    Long assessedBy,
    String carStatusUpdated
) {}
