package com.akif.payment.api;

import com.akif.shared.enums.CurrencyType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCapturedEvent(
        Long paymentId,
        Long rentalId,
        String customerEmail,
        BigDecimal amount,
        CurrencyType currency,
        String transactionId,
        LocalDateTime paymentDate) {
}
