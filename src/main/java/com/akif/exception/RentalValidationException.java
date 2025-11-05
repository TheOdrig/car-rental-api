package com.akif.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class RentalValidationException extends BaseException {

    public static final String ERROR_CODE = "RENTAL_VALIDATION_FAILED";
    private final List<String> validationErrors;

    public RentalValidationException(String message) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST);
        this.validationErrors = List.of(message);
    }

    public RentalValidationException(String message, List<String> validationErrors) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST);
        this.validationErrors = validationErrors;
    }
}