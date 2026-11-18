package com.akif.rental.internal.exception;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class RentalNotFoundException extends BaseException {

    public static final String ERROR_CODE = ErrorCode.RENTAL_NOT_FOUND.name();

    public RentalNotFoundException(String message) {
        super(ERROR_CODE, message, HttpStatus.NOT_FOUND);
    }

    public RentalNotFoundException(Long id) {
        super(ERROR_CODE, "Rental not found with id: " + id, HttpStatus.NOT_FOUND);
    }

    public RentalNotFoundException(String field, String value) {
        super(ERROR_CODE, String.format("Rental not found with %s: %s", field, value), HttpStatus.NOT_FOUND);
    }
}