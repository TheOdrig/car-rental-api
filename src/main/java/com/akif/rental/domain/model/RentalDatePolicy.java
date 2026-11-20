package com.akif.rental.domain.model;

import com.akif.rental.domain.exception.RentalValidationException;

import java.time.LocalDate;
import com.akif.shared.time.RentalDays;

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
        if (RentalDays.inclusiveDays(startDate, endDate) > maxRentalDays) {
            throw new RentalValidationException("Rental cannot be longer than " + maxRentalDays + " days");
        }
    }
}
