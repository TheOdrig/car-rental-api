package com.akif.payment.internal.service.gateway;

import com.akif.payment.api.PaymentResult;

public record ChargeResult(PaymentResult result, boolean definitiveDecline) {

    public static ChargeResult success(String transactionId, String message) {
        return new ChargeResult(PaymentResult.success(transactionId, message), false);
    }

    public static ChargeResult declined(String message) {
        return new ChargeResult(PaymentResult.failure(message), true);
    }

    public static ChargeResult ambiguous(String message) {
        return new ChargeResult(PaymentResult.failure(message), false);
    }

    public boolean success() {
        return result.success();
    }
}
