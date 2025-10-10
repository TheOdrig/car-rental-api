package com.akif.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class CarValidationException extends BaseException {

    public static final String ERROR_CODE = "CAR_VALIDATION_FAILED";

    private final List<String> validationErrors;

    public CarValidationException(String message) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST);
        this.validationErrors = List.of(message);
    }

    public CarValidationException(List<String> validationErrors) {
        super(ERROR_CODE, "Car validation failed: " + (validationErrors != null ? String.join(", ", validationErrors) : ""), HttpStatus.BAD_REQUEST);
        this.validationErrors = validationErrors;
    }

    public CarValidationException(String message, HttpStatus httpStatus) {
        super(ERROR_CODE, message, httpStatus);
        this.validationErrors = List.of(message);
    }

    public CarValidationException(List<String> validationErrors, HttpStatus httpStatus) {
        super(ERROR_CODE, "Car validation failed: " + (validationErrors != null ? String.join(", ", validationErrors) : ""), httpStatus);
        this.validationErrors = validationErrors;
    }

    @Override
    public String toString() {
        return String.format("%s[errorCode=%s, validationErrors=%s]",
                getClass().getSimpleName(), getErrorCode(), validationErrors);
    }
}
