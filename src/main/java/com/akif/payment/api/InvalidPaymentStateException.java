package com.akif.payment.api;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidPaymentStateException extends BaseException {

    public static final String ERROR_CODE = ErrorCode.INVALID_PAYMENT_STATE.name();

    public InvalidPaymentStateException(String message) {
        super(ERROR_CODE, message, HttpStatus.CONFLICT);
    }
}
