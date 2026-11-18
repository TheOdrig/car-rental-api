package com.akif.shared.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends BaseException {
    
    public static final String ERROR_CODE = ErrorCode.INVALID_TOKEN.name();
    
    public InvalidTokenException(String message) {
        super(ERROR_CODE, message, HttpStatus.UNAUTHORIZED);
    }

    public InvalidTokenException(String message, HttpStatus httpStatus) {
        super(ERROR_CODE, message, httpStatus);
    }
}
