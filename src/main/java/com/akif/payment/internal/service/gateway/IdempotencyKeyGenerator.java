package com.akif.payment.internal.service.gateway;

import com.akif.payment.internal.config.InstallationIdentity;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyKeyGenerator {

    private static final String APP = "car-rental";

    private final InstallationIdentity installation;

    public IdempotencyKeyGenerator(InstallationIdentity installation) {
        this.installation = installation;
    }

    private String prefix() {
        return APP + ":" + installation.id();
    }

    public String forCustomer(Long rentalId) {
        return String.format("%s:rental-%d:customer", prefix(), rentalId);
    }

    public String forIntent(Long rentalId, Long paymentId, int attempt) {
        return key(rentalId, paymentId, "intent-create", attempt);
    }

    public String forCapture(Long rentalId, Long paymentId, int attempt) {
        return key(rentalId, paymentId, "capture", attempt);
    }

    public String forOrphanCancel(String intentId) {
        return prefix() + ":orphan-cancel:" + intentId;
    }

    public String forCancel(Long rentalId, Long paymentId, int attempt) {
        return key(rentalId, paymentId, "cancel", attempt);
    }

    public String forPenaltyCharge(Long rentalId, Long paymentId, int attempt) {
        return key(rentalId, paymentId, "penalty", attempt);
    }

    public String forDamageCharge(Long damageId, int attempt) {
        return String.format("%s:damage-%d:charge:%d", prefix(), damageId, attempt);
    }

    public String forRefundOperation(String operationKey, int failedAttempts) {
        String base = prefix() + ":" + operationKey;
        return failedAttempts <= 0 ? base : base + ":retry-" + failedAttempts;
    }

    private String key(Long rentalId, Long paymentId, String operation, int version) {
        return String.format("%s:rental-%d:payment-%d:%s:%d", prefix(), rentalId, paymentId, operation, version);
    }
}
