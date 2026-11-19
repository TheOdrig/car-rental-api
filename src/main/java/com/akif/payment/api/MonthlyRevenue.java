package com.akif.payment.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyRevenue(
        YearMonth month,
        CurrencyType currency,
        BigDecimal revenue,
        int paymentCount) {
}
