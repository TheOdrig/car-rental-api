package com.akif.damage.internal.dto.damage.request;

import com.akif.damage.api.DamageCategory;
import com.akif.damage.api.DamageSeverity;
import com.akif.damage.api.DamageStatus;

import java.time.LocalDate;

public record DamageSearchFilterDto (
    
    LocalDate startDate,
    LocalDate endDate,
    DamageSeverity severity,
    DamageCategory category,
    DamageStatus status,
    Long carId
) {}

