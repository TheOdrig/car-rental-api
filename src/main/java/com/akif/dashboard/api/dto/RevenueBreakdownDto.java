package com.akif.dashboard.api.dto;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;

public record RevenueBreakdownDto(
        CurrencyType currency,
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
