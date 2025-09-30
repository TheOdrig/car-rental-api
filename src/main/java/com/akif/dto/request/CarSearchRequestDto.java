package com.akif.dto.request;

import com.akif.enums.CarStatusType;
import com.akif.enums.CurrencyType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarSearchRequestDto {

    @Size(max = 100, message = "Search term cannot exceed 100 characters")
    private String searchTerm;

    @Size(max = 50, message = "Brand filter cannot exceed 50 characters")
    private String brand;

    @Size(max = 50, message = "Model filter cannot exceed 50 characters")
    private String model;

    @Min(value = 1900, message = "Minimum production year must be at least 1900")
    private Integer minProductionYear;

    @Max(value = 2030, message = "Maximum production year cannot exceed 2030")
    private Integer maxProductionYear;

    @DecimalMin(value = "0.0", message = "Minimum price cannot be negative")
    private BigDecimal minPrice;

    @DecimalMax(value = "999999999.99", message = "Maximum price cannot exceed 999,999,999.99")
    private BigDecimal maxPrice;

    private CurrencyType currencyType;
    private CarStatusType carStatusType;

    @Size(max = 20, message = "Engine type filter cannot exceed 20 characters")
    private String engineType;

    @Size(max = 20, message = "Fuel type filter cannot exceed 20 characters")
    private String fuelType;

    @Size(max = 20, message = "Transmission type filter cannot exceed 20 characters")
    private String transmissionType;

    @Size(max = 20, message = "Body type filter cannot exceed 20 characters")
    private String bodyType;

    @Size(max = 30, message = "Color filter cannot exceed 30 characters")
    private String color;

    @Min(value = 0, message = "Minimum kilometer cannot be negative")
    private Long minKilometer;

    @Max(value = 999999, message = "Maximum kilometer cannot exceed 999,999 km")
    private Long maxKilometer;

    @Min(value = 2, message = "Minimum doors must be at least 2")
    private Integer minDoors;

    @Max(value = 6, message = "Maximum doors cannot exceed 6")
    private Integer maxDoors;

    @Min(value = 1, message = "Minimum seats must be at least 1")
    private Integer minSeats;

    @Max(value = 9, message = "Maximum seats cannot exceed 9")
    private Integer maxSeats;

    @DecimalMin(value = "1.0", message = "Minimum rating must be at least 1.0")
    private BigDecimal minRating;

    @DecimalMax(value = "5.0", message = "Maximum rating cannot exceed 5.0")
    private BigDecimal maxRating;

    private Boolean isFeatured;
    private Boolean isTestDriveAvailable;
    private Boolean isNew;
    private Boolean hasDamage;
    private Boolean needsService;
    private Boolean hasExpiredDocuments;


    @Pattern(regexp = "^(id|licensePlate|brand|model|productionYear|price|createTime|viewCount|likeCount|rating)$",
            message = "Invalid sort field")
    private String sortBy;

    @Pattern(regexp = "^(asc|desc)$", message = "Sort direction must be 'asc' or 'desc'")
    private String sortDirection = "asc";

    @Min(value = 0, message = "Page number cannot be negative")
    private Integer page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private Integer size = 20;
}
