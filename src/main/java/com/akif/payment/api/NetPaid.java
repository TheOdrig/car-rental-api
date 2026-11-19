package com.akif.payment.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;

public record NetPaid(CurrencyType currency, long rentalCount, BigDecimal amount) {
}
