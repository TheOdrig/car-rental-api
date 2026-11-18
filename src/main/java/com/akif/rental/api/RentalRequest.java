package com.akif.rental.internal.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

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
){

    @AssertTrue(message = "End date must be after start date")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }
}