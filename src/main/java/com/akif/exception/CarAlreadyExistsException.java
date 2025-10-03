package com.akif.exception;

public class CarAlreadyExistsException extends BaseException{

    public static final String ERROR_CODE = "CAR_ALREADY_EXISTS";

    public CarAlreadyExistsException(String message) {
        super(ERROR_CODE, message);
    }

    public CarAlreadyExistsException(String field, String value) {
        super(ERROR_CODE, String.format("Car already exists with %s: %s", field, value));
    }
}
