package com.akif.dto.damage.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DamageDisputeResolutionDto(
    
    @NotNull(message = "Adjusted repair cost is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Adjusted repair cost cannot be negative")
    BigDecimal adjustedRepairCost,
    
    @NotNull(message = "Adjusted customer liability is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Adjusted customer liability cannot be negative")
    BigDecimal adjustedCustomerLiability,
    
    @NotBlank(message = "Resolution notes are required")
    @Size(max = 1000, message = "Resolution notes cannot exceed 1000 characters")
    String resolutionNotes
) {}
