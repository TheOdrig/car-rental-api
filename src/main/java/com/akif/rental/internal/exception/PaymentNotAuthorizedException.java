package com.akif.rental.internal.exception;

import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class PaymentNotAuthorizedException extends BaseException {

    public static final String ERROR_CODE = "PAYMENT_NOT_AUTHORIZED";

    public PaymentNotAuthorizedException(String currentStatus) {
        super(ERROR_CODE, "Payment must be authorized before pickup. Current status: " + currentStatus,
                HttpStatus.CONFLICT);
    }
}
