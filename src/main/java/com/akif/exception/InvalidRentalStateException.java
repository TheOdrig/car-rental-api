package com.akif.exception;

import org.springframework.http.HttpStatus;

public class InvalidRentalStateException extends BaseException {

    public static final String ERROR_CODE = "INVALID_RENTAL_STATE";

    public InvalidRentalStateException(String message) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST);
    }

    public InvalidRentalStateException(String currentStatus, String requiredStatus) {
        super(ERROR_CODE,
                String.format("Invalid rental state. Current: %s, Required: %s", currentStatus, requiredStatus),
                HttpStatus.BAD_REQUEST);
    }
}
