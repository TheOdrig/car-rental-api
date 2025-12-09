package com.akif.dto.damage.request;

import com.akif.shared.enums.DamageCategory;
import com.akif.shared.enums.DamageSeverity;
import com.akif.shared.enums.DamageStatus;

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
