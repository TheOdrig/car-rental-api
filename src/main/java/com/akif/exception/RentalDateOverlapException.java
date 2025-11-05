package com.akif.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDate;

public class RentalDateOverlapException extends BaseException {

    public static final String ERROR_CODE = "RENTAL_DATE_OVERLAP";

    public RentalDateOverlapException(String message) {
        super(ERROR_CODE, message, HttpStatus.CONFLICT);
    }

    public RentalDateOverlapException(Long carId, LocalDate startDate, LocalDate endDate) {
        super(ERROR_CODE,
                String.format("Car with id %d is already rented between %s and %s", carId, startDate, endDate),
                HttpStatus.CONFLICT);
    }
}