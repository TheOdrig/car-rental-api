package com.akif.car.internal.exception;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.exception.BaseException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class AvailabilityValidationException extends BaseException {

    public static final String ERROR_CODE = ErrorCode.RENTAL_VALIDATION_FAILED.name();
    private final List<String> validationErrors;

    public AvailabilityValidationException(String message) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST);
        this.validationErrors = List.of(message);
    }
}
