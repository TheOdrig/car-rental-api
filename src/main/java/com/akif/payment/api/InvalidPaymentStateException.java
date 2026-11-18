package com.akif.payment.api;

import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidPaymentStateException extends BaseException {

    public static final String ERROR_CODE = "INVALID_PAYMENT_STATE";

    public InvalidPaymentStateException(String message) {
        super(ERROR_CODE, message, HttpStatus.CONFLICT);
    }
}
