package com.akif.payment.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;

/**
 * Public DTO for cross-module Payment data access.
 * Used by rental module to receive payment information via PaymentService public API.
 */
public record PaymentDto(
        Long id,
        Long rentalId,
        String userEmail,
        String carLicensePlate,
        BigDecimal amount,
        CurrencyType currency,
        PaymentStatus status,
        String paymentMethod,
        String transactionId,
        String stripeSessionId,
        String stripePaymentIntentId,
        String failureReason
) {
    
    public boolean isSuccessful() {
        return status != null && status.isSuccessful();
    }
    
    public boolean canRefund() {
        return status != null && status.canRefund();
    }
}
