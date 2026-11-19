package com.akif.payment.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;

public record PaymentSummary(
        Long paymentId,
        Long rentalId,
        PaymentStatus status,
        BigDecimal amount,
        CurrencyType currency,
        BigDecimal refundedAmount,
        String failureReason
) {
}
