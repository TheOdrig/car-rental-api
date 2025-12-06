package com.akif.dto.damage.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DamageDisputeRequestDto (
    
    @NotBlank(message = "Dispute reason is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    String reason,
    
    @Size(max = 1000, message = "Comments cannot exceed 1000 characters")
    String comments
) {}
