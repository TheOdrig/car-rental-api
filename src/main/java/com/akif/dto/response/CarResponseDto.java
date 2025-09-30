package com.akif.dto.response;

import com.akif.enums.CarStatusType;
import com.akif.enums.CurrencyType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CarResponseDto implements Serializable {

    private Long id;
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
    private Long viewCount;
    private Long likeCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long version;


    private Boolean isAvailable;
    private Boolean canBeSold;
    private Boolean canBeReserved;
    private Boolean requiresAttention;
    private String formattedPrice;
    private String formattedPriceWithSeparators;
    private Integer age;
    private Boolean isNew;
    private Boolean isOld;
    private String fullName;
    private String displayName;
    private Boolean hasDamage;
    private BigDecimal totalPrice;
    private Boolean needsService;
    private Boolean isInsuranceExpired;
    private Boolean isInspectionExpired;
    private Boolean hasExpiredDocuments;
}