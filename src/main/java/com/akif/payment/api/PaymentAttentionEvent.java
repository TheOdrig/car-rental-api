package com.akif.payment.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentAttentionEvent(
        Long paymentId,
        Long rentalId,
        Reason reason,
        BigDecimal amount,
        CurrencyType currency,
        LocalDateTime occurredAt) {

    public enum Reason {
        CHARGE_OUTCOME_UNKNOWN,
        LATE_CAPTURE_REFUNDED,
        DUPLICATE_PENALTY_REFUNDED,
        AUTO_REFUND_FAILED
    }
}
