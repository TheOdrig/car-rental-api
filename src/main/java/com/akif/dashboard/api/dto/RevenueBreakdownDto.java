package com.akif.dashboard.api.dto;

import java.math.BigDecimal;

public record RevenueBreakdownDto(
    BigDecimal rentalRevenue,
    BigDecimal penaltyRevenue,
    BigDecimal damageCharges,
    BigDecimal totalRevenue,
    BigDecimal rentalPercentage,
    BigDecimal penaltyPercentage,
    BigDecimal damagePercentage
) {}

