package com.akif.car.internal.service.availability;

import com.akif.car.internal.exception.AvailabilityValidationException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class RentalWindow {

    private RentalWindow() {
    }

    public static void validate(LocalDate startDate, LocalDate endDate, LocalDate today, int maxDays) {
        if (startDate.isBefore(today)) {
            throw new AvailabilityValidationException("Start date cannot be in the past");
        }
        if (endDate.isBefore(startDate)) {
            throw new AvailabilityValidationException("End date must be after start date");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) + 1 > maxDays) {
            throw new AvailabilityValidationException("Rental period cannot exceed " + maxDays + " days");
        }
    }
}
