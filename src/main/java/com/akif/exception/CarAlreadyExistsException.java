package com.akif.exception;

import org.springframework.http.HttpStatus;

public class CarAlreadyExistsException extends BaseException{

    public static final String ERROR_CODE = "CAR_ALREADY_EXISTS";

    public CarAlreadyExistsException(String message) {
        super(ERROR_CODE, message, HttpStatus.CONFLICT);
    }

    public CarAlreadyExistsException(String message, HttpStatus httpStatus) {
        super(ERROR_CODE, message, httpStatus);
    }

    public CarAlreadyExistsException(String field, String value) {
        super(ERROR_CODE, String.format("Car already exists with %s: %s", field, value), HttpStatus.CONFLICT);
    }
}
