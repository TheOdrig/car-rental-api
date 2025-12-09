package com.akif.dto.damage.request;

import com.akif.shared.enums.DamageCategory;
import com.akif.shared.enums.DamageSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record DamageReportRequestDto(
    
    @NotBlank(message = "Description cannot be blank")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    String description,
    
    @Size(max = 200, message = "Damage location cannot exceed 200 characters")
    String damageLocation,
    
    DamageSeverity initialSeverity,
    
    DamageCategory category
) {}
