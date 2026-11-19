package com.akif.rental.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;

public record EarlyReturnPreview(
        Long rentalId,
        BigDecimal amount,
        CurrencyType currency,
        int unusedDays) {
}
