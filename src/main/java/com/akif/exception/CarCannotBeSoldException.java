package com.akif.exception;

import org.springframework.http.HttpStatus;

public class CarCannotBeSoldException extends BaseException {

    public static final String ERROR_CODE = "CAR_CANNOT_BE_SOLD";

    public CarCannotBeSoldException(String message) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST);
    }

    public CarCannotBeSoldException(String message, HttpStatus httpStatus) {
        super(ERROR_CODE, message, httpStatus);
    }
}
