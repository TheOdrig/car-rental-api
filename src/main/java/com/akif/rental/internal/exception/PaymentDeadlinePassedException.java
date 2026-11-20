package com.akif.rental.internal.exception;

import com.akif.shared.exception.BaseException;
import com.akif.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

public class PaymentDeadlinePassedException extends BaseException {

    public static final String ERROR_CODE = ErrorCode.PAYMENT_DEADLINE_PASSED.name();
    public static final String DEADLINE_FIELD = "paymentDueBy";

    public PaymentDeadlinePassedException(Instant dueBy) {
        super(ERROR_CODE, "The payment deadline passed at " + dueBy, HttpStatus.CONFLICT);
        setDetails(Map.of(DEADLINE_FIELD, dueBy.toString()));
    }
}
