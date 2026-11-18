package com.akif.payment.internal.exception;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.exception.BaseException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class WebhookSignatureException extends BaseException {

    private final String eventId;

    public WebhookSignatureException(String eventId) {
        super(ErrorCode.WEBHOOK_SIGNATURE_INVALID.name(),
              String.format("Invalid webhook signature for event: %s", eventId),
              HttpStatus.BAD_REQUEST);
        this.eventId = eventId;
    }

    public WebhookSignatureException(String eventId, Throwable cause) {
        super(ErrorCode.WEBHOOK_SIGNATURE_INVALID.name(),
              String.format("Invalid webhook signature for event: %s", eventId),
              HttpStatus.BAD_REQUEST,
              cause);
        this.eventId = eventId;
    }
}
