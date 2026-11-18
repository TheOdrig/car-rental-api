package com.akif.payment.internal.service.gateway;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;

public record ChargeCommand(
        Long paymentId,
        Long rentalId,
        BigDecimal amount,
        CurrencyType currency,
        String customerId,
        String paymentMethodId,
        String description
) {
}
