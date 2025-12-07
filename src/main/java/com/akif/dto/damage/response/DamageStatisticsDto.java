package com.akif.dto.damage.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record DamageStatisticsDto(
    
    int totalDamages,
    int minorCount,
    int moderateCount,
    int majorCount,
    int totalLossCount,
    BigDecimal totalRepairCost,
    BigDecimal totalCustomerLiability,
    BigDecimal averageRepairCost,
    int disputedCount,
    int resolvedCount
) {}
