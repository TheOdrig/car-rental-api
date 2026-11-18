package com.akif.payment.api;

import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class PaymentNotFoundException extends BaseException {

    public static final String ERROR_CODE = "PAYMENT_NOT_FOUND";

    public PaymentNotFoundException(String message) {
        super(ERROR_CODE, message, HttpStatus.NOT_FOUND);
    }
}
