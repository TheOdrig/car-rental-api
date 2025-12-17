package com.akif.dashboard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenueDto(
    LocalDate date,
    BigDecimal revenue,
    int rentalCount
) {}
