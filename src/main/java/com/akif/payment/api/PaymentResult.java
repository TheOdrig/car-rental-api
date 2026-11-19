package com.akif.payment.api;

public record PaymentResult(
        boolean success,
        String transactionId,
        String message,
        boolean outcomeUnknown
) {
    public PaymentResult(boolean success, String transactionId, String message) {
        this(success, transactionId, message, false);
    }

    public static PaymentResult success(String transactionId, String message) {
        return new PaymentResult(true, transactionId, message, false);
    }

    public static PaymentResult failure(String message) {
        return new PaymentResult(false, null, message, false);
    }

    public static PaymentResult unknown(String message) {
        return new PaymentResult(false, null, message, true);
    }
}
