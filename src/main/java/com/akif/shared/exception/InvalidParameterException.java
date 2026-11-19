package com.akif.shared.exception;

import org.springframework.http.HttpStatus;

public class InvalidParameterException extends BaseException {

    public InvalidParameterException(String message) {
        super(ErrorCode.INVALID_PARAMETER.name(), message, HttpStatus.BAD_REQUEST);
    }
}
