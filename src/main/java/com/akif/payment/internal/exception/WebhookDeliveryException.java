package com.akif.payment.internal.exception;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class WebhookDeliveryException extends BaseException {

    private WebhookDeliveryException(String errorCode, String message, HttpStatus status) {
        super(errorCode, message, status);
    }

    public static WebhookDeliveryException missingSignature() {
        return new WebhookDeliveryException(ErrorCode.WEBHOOK_SIGNATURE_MISSING.name(),
                "Missing Stripe-Signature header", HttpStatus.BAD_REQUEST);
    }

    public static WebhookDeliveryException payloadTooLarge(long limitBytes) {
        return new WebhookDeliveryException(ErrorCode.WEBHOOK_PAYLOAD_TOO_LARGE.name(),
                "Webhook body exceeds " + limitBytes + " bytes", HttpStatus.CONTENT_TOO_LARGE);
    }

    public static WebhookDeliveryException processingFailed() {
        return new WebhookDeliveryException(ErrorCode.WEBHOOK_PROCESSING_FAILED.name(),
                "Webhook processing failed", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
