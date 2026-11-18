package com.akif.damage.internal.dto.damage.request;

import com.akif.damage.domain.enums.DamageCategory;
import com.akif.damage.api.DamageSeverity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DamageAssessmentRequest(
    
    @NotNull(message = "Severity is required")
    DamageSeverity severity,
    
    @NotNull(message = "Category is required")
    DamageCategory category,
    
    @NotNull(message = "Repair cost estimate is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Repair cost cannot be negative")
    BigDecimal repairCostEstimate,
    
    Boolean insuranceCoverage,
    
    BigDecimal insuranceDeductible,
    
    @Size(max = 1000, message = "Assessment notes cannot exceed 1000 characters")
    String assessmentNotes
) {}
