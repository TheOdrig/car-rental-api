package com.akif.car.internal.dto.request;

import com.akif.car.api.CarStatusType;
import com.akif.shared.enums.CurrencyType;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CarSearchRequest(
        @Size(max = 100, message = "Search term cannot exceed 100 characters") String searchTerm,
        @Size(max = 50, message = "Brand filter cannot exceed 50 characters") String brand,
        @Size(max = 50, message = "Model filter cannot exceed 50 characters") String model,
        @Min(value = 1900, message = "Minimum production year must be at least 1900") Integer minProductionYear,
        @Max(value = 2030, message = "Maximum production year cannot exceed 2030") Integer maxProductionYear,
        @DecimalMin(value = "0.0", message = "Minimum price cannot be negative") BigDecimal minPrice,
        @DecimalMax(value = "999999999.99", message = "Maximum price cannot exceed 999,999,999.99") BigDecimal maxPrice,
        CurrencyType currencyType,
        CarStatusType carStatusType,
        @Size(max = 20, message = "Engine type filter cannot exceed 20 characters") String engineType,
        @Size(max = 20, message = "Fuel type filter cannot exceed 20 characters") String fuelType,
        @Size(max = 20, message = "Transmission type filter cannot exceed 20 characters") String transmissionType,
        @Size(max = 20, message = "Body type filter cannot exceed 20 characters") String bodyType,
        @Size(max = 30, message = "Color filter cannot exceed 30 characters") String color,
        @Max(value = 999999, message = "Maximum kilometer cannot exceed 999,999 km") Long maxKilometer,
        @Min(value = 2, message = "Minimum doors must be at least 2") Integer minDoors,
        @Min(value = 1, message = "Minimum seats must be at least 1") Integer minSeats,
        @Max(value = 9, message = "Maximum seats cannot exceed 9") Integer maxSeats,
        @DecimalMin(value = "1.0", message = "Minimum rating must be at least 1.0") BigDecimal minRating,
        @Pattern(regexp = "^(id|licensePlate|brand|model|productionYear|price|createTime|rating)$", message = "Invalid sort field") String sortBy,
        @Pattern(regexp = "^(asc|desc)$", message = "Sort direction must be 'asc' or 'desc'") String sortDirection,
        @Min(value = 0, message = "Page number cannot be negative") Integer page,
        @Min(value = 1, message = "Page size must be at least 1") @Max(value = 100, message = "Page size cannot exceed 100") Integer size
) {

    public CarSearchRequest {
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
}
