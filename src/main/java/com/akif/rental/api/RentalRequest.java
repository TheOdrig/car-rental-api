package com.akif.rental.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RentalRequest(

    @NotNull(message = "Car ID cannot be null")
    @Positive(message = "Car ID must be positive")
    Long carId,

    @NotNull(message = "Start date cannot be null")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate startDate,

    @NotNull(message = "End date cannot be null")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate endDate,

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    String notes
) {
}
