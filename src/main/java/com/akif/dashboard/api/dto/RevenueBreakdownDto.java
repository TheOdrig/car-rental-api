package com.akif.dashboard.api.dto;

import java.math.BigDecimal;

public record RevenueBreakdownDto(
        BigDecimal rentalRevenue,
        BigDecimal penaltyRevenue,
        BigDecimal damageRecovered,
        BigDecimal damageRepairCosts,
        BigDecimal netDamageImpact,
        BigDecimal totalRevenue,
        BigDecimal netRevenue,
        BigDecimal rentalPercentage,
        BigDecimal penaltyPercentage,
        BigDecimal damagePercentage) {
}

