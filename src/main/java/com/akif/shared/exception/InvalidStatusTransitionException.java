package com.akif.shared.exception;

import org.springframework.http.HttpStatus;

public class InvalidStatusTransitionException extends BaseException {

    public static final String ERROR_CODE = ErrorCode.INVALID_STATUS_TRANSITION.name();

    public InvalidStatusTransitionException(String message) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST);
    }

    public InvalidStatusTransitionException(String message, HttpStatus httpStatus) {
        super(ERROR_CODE, message, httpStatus);
    }
}
