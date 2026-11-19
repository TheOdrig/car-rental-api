package com.akif.dashboard.api.dto;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenueDto(
    LocalDate date,
    CurrencyType currency,
    BigDecimal revenue,
    int paymentCount
) {}
