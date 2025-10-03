package com.akif.exception;

import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {

    private final String errorCode;
    private final String errorMessage;

    protected BaseException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    protected BaseException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return String.format("%s[errorCode=%s, errorMessage=%s]",
                getClass().getSimpleName(), errorCode, errorMessage);
    }
}
