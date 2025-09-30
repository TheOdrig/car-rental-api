package com.akif.dto.request;

import com.akif.enums.CarStatusType;
import com.akif.enums.CurrencyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CarRequestDto implements Serializable {

    private String licensePlate;
    private String vinNumber;
    private String brand;
    private String model;
    private Integer productionYear;
    private BigDecimal price;
    private CurrencyType currencyType;
    private BigDecimal damagePrice;
    private CarStatusType carStatusType;
    private String engineType;
    private BigDecimal engineDisplacement;
    private String fuelType;
    private String transmissionType;
    private String bodyType;
    private String color;
    private Long kilometer;
    private Integer doors;
    private Integer seats;
    private LocalDate registrationDate;
    private LocalDate lastServiceDate;
    private LocalDate nextServiceDate;
    private LocalDate insuranceExpiryDate;
    private LocalDate inspectionExpiryDate;
    private String notes;
    private String imageUrl;
    private String thumbnailUrl;
    private Boolean isFeatured;
    private Boolean isTestDriveAvailable;
    private BigDecimal rating;
}