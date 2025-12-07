package com.akif.dto.damage.request;

import com.akif.enums.DamageCategory;
import com.akif.enums.DamageSeverity;
import com.akif.enums.DamageStatus;

import java.time.LocalDate;

public record DamageSearchFilterDto (
    
    LocalDate startDate,
    LocalDate endDate,
    DamageSeverity severity,
    DamageCategory category,
    DamageStatus status,
    Long carId,
    Long customerId
) {}
