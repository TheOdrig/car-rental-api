package com.akif.exception;

import org.springframework.http.HttpStatus;

public class CarCannotBeReservedException extends BaseException{

    public static final String ERROR_CODE = "CAR_CANNOT_BE_RESERVED";

    public CarCannotBeReservedException(String message) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST);
    }

    public CarCannotBeReservedException(String message, HttpStatus httpStatus) {
        super(ERROR_CODE, message, httpStatus);
    }
}
