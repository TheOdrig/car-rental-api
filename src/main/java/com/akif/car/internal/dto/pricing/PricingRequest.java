package com.akif.car.internal.dto.pricing;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import com.akif.shared.handler.ReportsAsField;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record PricingRequest(

    @NotNull(message = "Car ID cannot be null")
    @Positive(message = "Car ID must be positive")
    Long carId,

    @NotNull(message = "Start date cannot be null")
    @FutureOrPresent(message = "Start date must be today or in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate startDate,

    @NotNull(message = "End date cannot be null")
    @FutureOrPresent(message = "End date must be today or in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate endDate

) {
    @JsonIgnore
    @Schema(hidden = true)
    @ReportsAsField("endDate")
    @AssertTrue(message = "End date must be after start date")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }
}
