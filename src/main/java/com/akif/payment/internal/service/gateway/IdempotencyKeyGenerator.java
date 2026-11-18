package com.akif.payment.internal.service.gateway;

import org.springframework.stereotype.Component;

@Component
public class IdempotencyKeyGenerator {

    private static final String PREFIX = "car-rental";

    public String forCustomer(Long rentalId) {
        return String.format("%s:rental-%d:customer", PREFIX, rentalId);
    }

    public String forIntent(Long rentalId, Long paymentId, int attempt) {
        return key(rentalId, paymentId, "intent-create", attempt);
    }

    public String forCapture(Long rentalId, Long paymentId, int attempt) {
        return key(rentalId, paymentId, "capture", attempt);
    }

    public String forCancel(Long rentalId, Long paymentId, int attempt) {
        return key(rentalId, paymentId, "cancel", attempt);
    }

    public String forRefund(Long rentalId, Long paymentId, int refundSequence) {
        return key(rentalId, paymentId, "refund", refundSequence);
    }

    public String forOffSessionCharge(Long rentalId, Long paymentId, int attempt) {
        return key(rentalId, paymentId, "charge", attempt);
    }

    private String key(Long rentalId, Long paymentId, String operation, int version) {
        return String.format("%s:rental-%d:payment-%d:%s:%d", PREFIX, rentalId, paymentId, operation, version);
    }
}
