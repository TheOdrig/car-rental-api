package com.akif.dashboard.api.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyRevenueDto(
    YearMonth month,
    BigDecimal revenue,
    int rentalCount,
    BigDecimal growthPercentage
) {}
