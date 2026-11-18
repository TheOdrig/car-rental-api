package com.akif.rental.internal.exception;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidRentalStateException extends BaseException {

    public static final String ERROR_CODE = ErrorCode.INVALID_RENTAL_STATE.name();

    public InvalidRentalStateException(String message) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST);
    }

    public InvalidRentalStateException(String currentStatus, String requiredStatus) {
        super(ERROR_CODE,
                String.format("Invalid rental state. Current: %s, Required: %s", currentStatus, requiredStatus),
                HttpStatus.BAD_REQUEST);
    }
}
