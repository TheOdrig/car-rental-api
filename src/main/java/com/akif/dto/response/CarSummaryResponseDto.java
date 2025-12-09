package com.akif.dto.response;

import com.akif.shared.enums.CarStatusType;
import com.akif.shared.enums.CurrencyType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CarSummaryResponseDto {

    private Long id;
    private String licensePlate;
    private String brand;
    private String model;
    private Integer productionYear;
    private String formattedPrice;
    private CurrencyType currencyType;
    private CarStatusType carStatusType;
    private String color;
    private Long kilometer;
    private String thumbnailUrl;
    private Boolean isFeatured;
    private BigDecimal rating;
    private Long viewCount;
    private Long likeCount;

    private Integer age;
    private String fullName;
    private String displayName;
    private Boolean isNew;
    private Boolean isAvailable;
}
