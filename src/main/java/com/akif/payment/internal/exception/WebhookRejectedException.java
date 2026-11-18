package com.akif.payment.internal.exception;

public class WebhookRejectedException extends RuntimeException {

    public WebhookRejectedException(String message) {
        super(message);
    }
}
