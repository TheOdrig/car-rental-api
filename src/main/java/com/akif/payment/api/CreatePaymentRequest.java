package com.akif.payment.api;

import com.akif.shared.enums.CurrencyType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull Long rentalId,
        String userEmail,
        String carLicensePlate,
        @NotNull @Positive BigDecimal amount,
        @NotNull CurrencyType currency,
        String paymentMethod
) {
    
    public CreatePaymentRequest {
        paymentMethod = paymentMethod != null ? paymentMethod : "PENALTY";
    }
}
