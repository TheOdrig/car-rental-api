package com.akif.exception;

import org.springframework.http.HttpStatus;

public class LateReturnException extends BaseException {
    
    public LateReturnException(String message) {
        super("LATE_RETURN_ERROR", message, HttpStatus.BAD_REQUEST);
    }
    
    public LateReturnException(String message, Throwable cause) {
        super("LATE_RETURN_ERROR", message, HttpStatus.BAD_REQUEST, cause);
    }
}
