package com.akif.auth.internal.exception;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidPasswordException extends BaseException {

    public static final String ERROR_CODE = ErrorCode.INVALID_PASSWORD.name();

    public InvalidPasswordException(String message) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST);
    }
}
