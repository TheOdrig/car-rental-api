package com.akif.shared.exception;

import org.springframework.http.HttpStatus;

public class TokenExpiredException extends BaseException {
    
    public static final String ERROR_CODE = ErrorCode.TOKEN_EXPIRED.name();
    
    public TokenExpiredException(String message) {
        super(ERROR_CODE, message, HttpStatus.UNAUTHORIZED);
    }

    public TokenExpiredException(String message, HttpStatus httpStatus) {
        super(ERROR_CODE, message, httpStatus);
    }
}
