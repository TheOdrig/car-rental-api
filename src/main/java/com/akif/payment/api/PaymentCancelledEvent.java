package com.akif.payment.api;

import com.akif.shared.enums.CurrencyType;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentCancelledEvent(
        Long paymentId,
        Long rentalId,
        String customerEmail,
        BigDecimal amount,
        CurrencyType currency,
        Instant occurredAt) {
}
