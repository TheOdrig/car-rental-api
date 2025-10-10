package com.akif.exception;

import org.springframework.http.HttpStatus;

public class CarOperationNotAllowedException extends BaseException {

    public static final String ERROR_CODE = "CAR_OPERATION_NOT_ALLOWED";

    public CarOperationNotAllowedException(String message) {
        super(ERROR_CODE, message, HttpStatus.FORBIDDEN);
    }

    public CarOperationNotAllowedException(String message, HttpStatus httpStatus) {
        super(ERROR_CODE, message, httpStatus);
    }
}
