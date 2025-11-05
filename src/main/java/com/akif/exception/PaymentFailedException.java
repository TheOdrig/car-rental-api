package com.akif.exception;

import org.springframework.http.HttpStatus;

public class PaymentFailedException extends BaseException {

    public static final String ERROR_CODE = "PAYMENT_FAILED";

    public PaymentFailedException(String message) {
        super(ERROR_CODE, message, HttpStatus.PAYMENT_REQUIRED);
    }

    public PaymentFailedException(String transactionId, String reason) {
        super(ERROR_CODE,
                String.format("Payment failed for transaction %s. Reason: %s", transactionId, reason),
                HttpStatus.PAYMENT_REQUIRED);
    }
}