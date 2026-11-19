package com.akif.payment.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PenaltyPaymentCapturedEvent(
        Long paymentId,
        Long rentalId,
        BigDecimal amount,
        CurrencyType currency,
        LocalDateTime capturedAt) {
}
