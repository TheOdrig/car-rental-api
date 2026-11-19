package com.akif.payment.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;

public record CollectedRevenue(
        PaymentCategory category,
        CurrencyType currency,
        BigDecimal amount,
        long paymentCount) {
}
