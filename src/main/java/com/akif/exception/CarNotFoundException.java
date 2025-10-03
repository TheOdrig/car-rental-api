package com.akif.exception;

public class CarNotFoundException extends BaseException{

    public static final String  ERROR_CODE = "CAR_NOT_FOUND";

    public CarNotFoundException(String message) {
        super(ERROR_CODE, message);
    }

    public CarNotFoundException(Long id) {
        super(ERROR_CODE, "Car not found with id: " + id);
    }

    public CarNotFoundException(String field, String value) {
        super(ERROR_CODE, String.format("Car not found with %s: %s", field, value));
    }
}
