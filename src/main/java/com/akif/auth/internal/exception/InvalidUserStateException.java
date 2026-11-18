package com.akif.auth.internal.exception;

import com.akif.shared.exception.BaseException;
import com.akif.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class InvalidUserStateException extends BaseException {

    public static final String ERROR_CODE = ErrorCode.INVALID_USER_STATE.name();

    public InvalidUserStateException(String message) {
        super(ERROR_CODE, message, HttpStatus.CONFLICT);
    }
}
