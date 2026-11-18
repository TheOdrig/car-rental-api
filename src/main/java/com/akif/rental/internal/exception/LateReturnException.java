package com.akif.rental.internal.exception;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class LateReturnException extends BaseException {
    
    public LateReturnException(String message) {
        super(ErrorCode.LATE_RETURN_ERROR.name(), message, HttpStatus.BAD_REQUEST);
    }
    
    public LateReturnException(String message, Throwable cause) {
        super(ErrorCode.LATE_RETURN_ERROR.name(), message, HttpStatus.BAD_REQUEST, cause);
    }
}
