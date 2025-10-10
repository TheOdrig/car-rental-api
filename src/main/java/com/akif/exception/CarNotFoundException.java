package com.akif.exception;

import org.springframework.http.HttpStatus;

public class CarNotFoundException extends BaseException{

    public static final String  ERROR_CODE = "CAR_NOT_FOUND";

    public CarNotFoundException(String message) {
        super(ERROR_CODE, message, HttpStatus.NOT_FOUND);
    }

    public CarNotFoundException(String message, HttpStatus httpStatus) {
        super(ERROR_CODE, message, httpStatus);
    }

    public CarNotFoundException(Long id) {
        super(ERROR_CODE, "Car not found with id: " + id, HttpStatus.NOT_FOUND);
    }

    public CarNotFoundException(String field, String value) {
        super(ERROR_CODE, String.format("Car not found with %s: %s", field, value), HttpStatus.NOT_FOUND);
    }

    public CarNotFoundException(Long id, HttpStatus httpStatus) {
        super(ERROR_CODE, "Car not found with id: " + id, httpStatus);
    }

    public CarNotFoundException(String field, String value, HttpStatus httpStatus) {
        super(ERROR_CODE, String.format("Car not found with %s: %s", field, value), httpStatus);
    }
}
