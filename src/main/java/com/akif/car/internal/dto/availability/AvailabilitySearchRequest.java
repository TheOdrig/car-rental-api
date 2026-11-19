package com.akif.car.internal.dto.availability;

import com.akif.shared.enums.CurrencyType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import com.akif.shared.handler.ReportsAsField;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder(toBuilder = true)
public record AvailabilitySearchRequest(
        @NotNull(message = "Start date cannot be null") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @NotNull(message = "End date cannot be null") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
        String brand,
        String model,
        String fuelType,
        String transmissionType,
        String bodyType,
        @Min(value = 1, message = "Minimum seats must be at least 1") Integer minSeats,
        @DecimalMin(value = "0.0", inclusive = false, message = "Minimum price must be positive") BigDecimal minPrice,
        @DecimalMin(value = "0.0", inclusive = false, message = "Maximum price must be positive") BigDecimal maxPrice,
        @Min(value = 1900, message = "Minimum production year must be at least 1900") Integer minProductionYear,
        @Min(value = 1900, message = "Maximum production year must be at least 1900") Integer maxProductionYear,
        CurrencyType targetCurrency,
        String sortBy,
        String sortDirection,
        @Min(value = 0, message = "Page number must be non-negative") Integer page,
        @Min(value = 1, message = "Page size must be at least 1") @Max(value = 100, message = "Page size cannot exceed 100") Integer size
) {

    public AvailabilitySearchRequest {
        if (sortBy == null) {
            sortBy = "price";
        }
        if (sortDirection == null) {
            sortDirection = "asc";
        }
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 20;
        }
    }

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

    @JsonIgnore

    @Schema(hidden = true)

    @ReportsAsField("maxPrice")
    @AssertTrue(message = "Maximum price must be greater than minimum price")
    public boolean isMaxPriceGreaterThanMinPrice() {
        if (minPrice == null || maxPrice == null) {
            return true;
        }
        return maxPrice.compareTo(minPrice) > 0;
    }

    @JsonIgnore

    @Schema(hidden = true)

    @ReportsAsField("maxProductionYear")
    @AssertTrue(message = "Maximum production year must be greater than or equal to minimum production year")
    public boolean isMaxYearGreaterThanMinYear() {
        if (minProductionYear == null || maxProductionYear == null) {
            return true;
        }
        return maxProductionYear >= minProductionYear;
    }
}
