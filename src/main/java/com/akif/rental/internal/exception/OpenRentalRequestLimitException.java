package com.akif.rental.internal.exception;

import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class OpenRentalRequestLimitException extends BaseException {

    public static final String ERROR_CODE = "OPEN_RENTAL_REQUEST_LIMIT_EXCEEDED";

    public OpenRentalRequestLimitException(int limit) {
        super(ERROR_CODE,
                "You already have " + limit + " open rental requests. Cancel one or wait for approval before requesting another.",
                HttpStatus.CONFLICT);
    }
}
