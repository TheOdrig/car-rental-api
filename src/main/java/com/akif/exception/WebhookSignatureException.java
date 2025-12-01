package com.akif.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class WebhookSignatureException extends BaseException {

    private final String eventId;

    public WebhookSignatureException(String eventId) {
        super("WEBHOOK_SIGNATURE_INVALID",
              String.format("Invalid webhook signature for event: %s", eventId),
              HttpStatus.BAD_REQUEST);
        this.eventId = eventId;
    }

    public WebhookSignatureException(String eventId, Throwable cause) {
        super("WEBHOOK_SIGNATURE_INVALID",
              String.format("Invalid webhook signature for event: %s", eventId),
              HttpStatus.BAD_REQUEST,
              cause);
        this.eventId = eventId;
    }
}
