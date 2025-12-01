package com.akif.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class StripeIntegrationException extends BaseException {

    private final String stripeErrorCode;
    private final String stripeErrorMessage;

    public StripeIntegrationException(String stripeErrorCode, String stripeErrorMessage) {
        super("STRIPE_INTEGRATION_ERROR", 
              String.format("Stripe API error: %s - %s", stripeErrorCode, stripeErrorMessage),
              HttpStatus.BAD_GATEWAY);
        this.stripeErrorCode = stripeErrorCode;
        this.stripeErrorMessage = stripeErrorMessage;
    }

    public StripeIntegrationException(String stripeErrorCode, String stripeErrorMessage, Throwable cause) {
        super("STRIPE_INTEGRATION_ERROR",
              String.format("Stripe API error: %s - %s", stripeErrorCode, stripeErrorMessage),
              HttpStatus.BAD_GATEWAY,
              cause);
        this.stripeErrorCode = stripeErrorCode;
        this.stripeErrorMessage = stripeErrorMessage;
    }
}
