package com.akif.dto.request;

import com.akif.enums.CarStatusType;
import com.akif.enums.CurrencyType;
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

    private String searchTerm;
    private String brand;
    private String model;
    private Integer minProductionYear;
    private Integer maxProductionYear;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private CurrencyType currencyType;
    private CarStatusType carStatusType;
    private String engineType;
    private String fuelType;
    private String transmissionType;
    private String bodyType;
    private String color;
    private Long minKilometer;
    private Long maxKilometer;
    private Integer minDoors;
    private Integer maxDoors;
    private Integer minSeats;
    private Integer maxSeats;
    private BigDecimal minRating;
    private BigDecimal maxRating;
    private Boolean isFeatured;
    private Boolean isTestDriveAvailable;
    private Boolean isNew;
    private Boolean hasDamage;
    private Boolean needsService;
    private Boolean hasExpiredDocuments;


    private String sortBy;
    private String sortDirection = "asc";
    private Integer page = 0;
    private Integer size = 20;
}
