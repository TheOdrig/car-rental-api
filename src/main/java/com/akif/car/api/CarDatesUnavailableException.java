package com.akif.car.api;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

public class CarDatesUnavailableException extends BaseException {

    public static final String ERROR_CODE = ErrorCode.CAR_DATES_UNAVAILABLE.name();

    private CarDatesUnavailableException(String message) {
        super(ERROR_CODE, message, HttpStatus.CONFLICT);
    }

    public static CarDatesUnavailableException stillInUse(Long carId) {
        return new CarDatesUnavailableException(
                String.format("Car with id %d is still out on another rental", carId));
    }

    public static CarDatesUnavailableException bookedForDates(Long carId, LocalDate startDate, LocalDate endDate) {
        return new CarDatesUnavailableException(
                String.format("Car with id %d is already booked between %s and %s", carId, startDate, endDate));
    }
}
