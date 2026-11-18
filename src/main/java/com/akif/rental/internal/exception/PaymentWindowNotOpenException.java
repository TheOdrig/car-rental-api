package com.akif.rental.internal.exception;

import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Map;

public class PaymentWindowNotOpenException extends BaseException {

    public static final String ERROR_CODE = "PAYMENT_WINDOW_NOT_OPEN";
    public static final String OPENS_ON_FIELD = "paymentOpensOn";

    public PaymentWindowNotOpenException(LocalDate opensOn) {
        super(ERROR_CODE, "Payment opens on " + opensOn, HttpStatus.CONFLICT);
        setDetails(Map.of(OPENS_ON_FIELD, opensOn.toString()));
    }
}
