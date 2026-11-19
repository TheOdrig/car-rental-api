package com.akif.dashboard.api.dto;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyRevenueDto(
    YearMonth month,
    CurrencyType currency,
    BigDecimal revenue,
    int paymentCount,
    BigDecimal growthPercentage
) {}
