package com.akif.damage.internal.dto.damage.request;

import com.akif.damage.domain.enums.DamageCategory;
import com.akif.damage.api.DamageSeverity;
import com.akif.damage.domain.enums.DamageStatus;

import java.time.LocalDate;

public record DamageSearchFilterDto (
    
    LocalDate startDate,
    LocalDate endDate,
    DamageSeverity severity,
    DamageCategory category,
    DamageStatus status,
    Long carId
) {}

