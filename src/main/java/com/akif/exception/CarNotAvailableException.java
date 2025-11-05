package com.akif.exception;

import org.springframework.http.HttpStatus;

public class CarNotAvailableException extends BaseException {

    public static final String ERROR_CODE = "CAR_NOT_AVAILABLE";

    public CarNotAvailableException(String message) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST);
    }

    public CarNotAvailableException(Long carId) {
        super(ERROR_CODE, "Car with id " + carId + " is not available for rental", HttpStatus.BAD_REQUEST);
    }

    public CarNotAvailableException(Long carId, String reason) {
        super(ERROR_CODE,
                String.format("Car with id %d is not available for rental. Reason: %s", carId, reason),
                HttpStatus.BAD_REQUEST);
    }
}