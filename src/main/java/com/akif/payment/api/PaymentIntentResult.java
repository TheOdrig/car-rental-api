package com.akif.payment.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;

public record PaymentIntentResult(
        Long paymentId,
        Long rentalId,
        String paymentIntentId,
        String clientSecret,
        String publishableKey,
        String status,
        BigDecimal amount,
        CurrencyType currency
) {
    @Override
    public String toString() {
        return "PaymentIntentResult[paymentId=" + paymentId + ", rentalId=" + rentalId + ", paymentIntentId=" + paymentIntentId + ", clientSecret=[REDACTED], publishableKey=" + publishableKey + ", status=" + status + ", amount=" + amount + ", currency=" + currency + "]";
    }
}
