package com.akif.payment.internal.service;

public record RefundRequiredEvent(Long paymentId, Cause cause) {

    public enum Cause {
        LATE_CAPTURE,
        DUPLICATE_PENALTY
    }
}
