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

    public String forOrphanCancel(String intentId) {
        return PREFIX + ":orphan-cancel:" + intentId;
    }

    public String forCancel(Long rentalId, Long paymentId, int attempt) {
        return key(rentalId, paymentId, "cancel", attempt);
    }

    public String forPenaltyCharge(Long rentalId, int attempt) {
        return String.format("%s:rental-%d:penalty:%d", PREFIX, rentalId, attempt);
    }

    public String forDamageCharge(Long damageId, int attempt) {
        return String.format("%s:damage-%d:charge:%d", PREFIX, damageId, attempt);
    }

    public String forRefundOperation(String operationKey, int failedAttempts) {
        String base = PREFIX + ":" + operationKey;
        return failedAttempts <= 0 ? base : base + ":retry-" + failedAttempts;
    }

    private String key(Long rentalId, Long paymentId, String operation, int version) {
        return String.format("%s:rental-%d:payment-%d:%s:%d", PREFIX, rentalId, paymentId, operation, version);
    }
}
