package com.akif.payment.internal.service.gateway;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;

public record IntentCommand(
        Long paymentId,
        Long rentalId,
        BigDecimal amount,
        CurrencyType currency,
        String customerEmail,
        String customerId,
        String description,
        String customerIdempotencyKey
) {
}
