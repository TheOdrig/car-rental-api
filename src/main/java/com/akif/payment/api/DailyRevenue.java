package com.akif.payment.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenue(
        LocalDate date,
        CurrencyType currency,
        BigDecimal revenue,
        int paymentCount) {
}
