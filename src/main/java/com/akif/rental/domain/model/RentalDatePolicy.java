package com.akif.rental.domain.model;

import com.akif.rental.internal.exception.RentalValidationException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record RentalDatePolicy(int maxLeadDays, int maxRentalDays) {

    public void validate(LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (startDate.isBefore(today)) {
            throw new RentalValidationException("Start date cannot be in the past");
        }
        if (!endDate.isAfter(startDate)) {
            throw new RentalValidationException("End date must be after start date");
        }
        if (startDate.isAfter(today.plusDays(maxLeadDays))) {
            throw new RentalValidationException("Start date cannot be more than " + maxLeadDays + " days ahead");
        }
        if (inclusiveDays(startDate, endDate) > maxRentalDays) {
            throw new RentalValidationException("Rental cannot be longer than " + maxRentalDays + " days");
        }
    }

    public static int inclusiveDays(LocalDate startDate, LocalDate endDate) {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
}
