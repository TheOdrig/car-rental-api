package com.akif.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BaseException extends RuntimeException {

    private final String errorCode;
    private final String errorMessage;
    private final HttpStatus httpStatus;
    private java.util.Map<String, Object> details = java.util.Map.of();
    private java.util.List<com.akif.shared.handler.FieldProblem> fieldErrors = java.util.List.of();

    protected BaseException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    protected BaseException(String errorCode, String errorMessage, HttpStatus httpStatus) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.httpStatus = httpStatus;
    }

    protected BaseException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    protected BaseException(String errorCode, String errorMessage, HttpStatus httpStatus, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.httpStatus = httpStatus;
    }

    protected void setFieldError(String field, String message) {
        this.fieldErrors = java.util.List.of(new com.akif.shared.handler.FieldProblem(field, message));
    }

    protected void setDetails(java.util.Map<String, Object> details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return String.format("%s[errorCode=%s, errorMessage=%s, httpStatus=%s]",
                getClass().getSimpleName(), errorCode, errorMessage, httpStatus);
    }
}
