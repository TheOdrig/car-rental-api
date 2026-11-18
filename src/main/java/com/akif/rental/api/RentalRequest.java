package com.akif.rental.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record RentalRequest(

    @NotNull(message = "Car ID cannot be null")
    @Positive(message = "Car ID must be positive")
    Long carId,

    @NotNull(message = "Start date cannot be null")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @FutureOrPresent(message = "Start date must be today or in the future")
    LocalDate startDate,

    @NotNull(message = "End date cannot be null")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Future(message = "End date must be in the future")
    LocalDate endDate,

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    String notes
) {

    public static final int MAX_RENTAL_DAYS = 30;
    public static final int MAX_LEAD_DAYS = 180;

    @AssertTrue(message = "End date must be after start date")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }

    @AssertTrue(message = "Rental cannot be longer than 30 days")
    public boolean isWithinMaxRentalLength() {
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            return true;
        }
        return ChronoUnit.DAYS.between(startDate, endDate) + 1 <= MAX_RENTAL_DAYS;
    }

    @AssertTrue(message = "Start date cannot be more than 180 days ahead")
    public boolean isWithinMaxLeadTime() {
        if (startDate == null) {
            return true;
        }
        return !startDate.isAfter(LocalDate.now().plusDays(MAX_LEAD_DAYS));
    }
}
