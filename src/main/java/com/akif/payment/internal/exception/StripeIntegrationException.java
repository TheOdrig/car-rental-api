package com.akif.payment.internal.exception;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.exception.BaseException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class StripeIntegrationException extends BaseException {

    private final String stripeErrorCode;
    private final String stripeErrorMessage;

    public StripeIntegrationException(String stripeErrorCode, String stripeErrorMessage) {
        super(ErrorCode.STRIPE_INTEGRATION_ERROR.name(), 
              String.format("Stripe API error: %s - %s", stripeErrorCode, stripeErrorMessage),
              HttpStatus.BAD_GATEWAY);
        this.stripeErrorCode = stripeErrorCode;
        this.stripeErrorMessage = stripeErrorMessage;
    }

    public StripeIntegrationException(String stripeErrorCode, String stripeErrorMessage, Throwable cause) {
        super(ErrorCode.STRIPE_INTEGRATION_ERROR.name(),
              String.format("Stripe API error: %s - %s", stripeErrorCode, stripeErrorMessage),
              HttpStatus.BAD_GATEWAY,
              cause);
        this.stripeErrorCode = stripeErrorCode;
        this.stripeErrorMessage = stripeErrorMessage;
    }
}
