package com.akif.auth.internal.exception;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends BaseException {
    
    public static final String ERROR_CODE = ErrorCode.USER_ALREADY_EXISTS.name();
    
    public UserAlreadyExistsException(String message) {
        super(ERROR_CODE, message, HttpStatus.CONFLICT);
    }

    public UserAlreadyExistsException(String message, HttpStatus httpStatus) {
        super(ERROR_CODE, message, httpStatus);
    }

    public UserAlreadyExistsException(String field, String value) {
        super(ERROR_CODE, String.format("User already exists with %s: %s", field, value), HttpStatus.CONFLICT);
    }

    public UserAlreadyExistsException(String field, String value, HttpStatus httpStatus) {
        super(ERROR_CODE, String.format("User already exists with %s: %s", field, value), httpStatus);
    }
}
