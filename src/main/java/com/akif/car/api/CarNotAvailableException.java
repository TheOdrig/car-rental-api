package com.akif.car.api;

import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

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

    private CarNotAvailableException(String message, HttpStatus status) {
        super(ERROR_CODE, message, status);
    }

    public static CarNotAvailableException stillInUse(Long carId) {
        return new CarNotAvailableException(
                String.format("Car with id %d is still out on another rental", carId),
                HttpStatus.CONFLICT);
    }

    public static CarNotAvailableException bookedForDates(Long carId, LocalDate startDate, LocalDate endDate) {
        return new CarNotAvailableException(
                String.format("Car with id %d is already booked between %s and %s", carId, startDate, endDate),
                HttpStatus.CONFLICT);
    }
}
