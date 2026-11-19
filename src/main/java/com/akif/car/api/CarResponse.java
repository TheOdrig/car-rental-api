package com.akif.car.api;

import com.akif.shared.enums.CurrencyType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CarResponse(
        Long id,
        String licensePlate,
        String vinNumber,
        String brand,
        String model,
        Integer productionYear,
        BigDecimal price,
        CurrencyType currencyType,
        CarStatusType carStatusType,
        String engineType,
        BigDecimal engineDisplacement,
        String fuelType,
        String transmissionType,
        String bodyType,
        String color,
        Long kilometer,
        Integer doors,
        Integer seats,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate registrationDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate lastServiceDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate nextServiceDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate insuranceExpiryDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate inspectionExpiryDate,
        String notes,
        String imageUrl,
        String thumbnailUrl,
        BigDecimal rating,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC") LocalDateTime createTime,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC") LocalDateTime updateTime,
        Long version,
        String formattedPrice,
        String formattedPriceWithSeparators,
        Integer age,
        String fullName,
        String displayName,
        BigDecimal convertedPrice,
        CurrencyType displayCurrency,
        BigDecimal exchangeRate,
        String rateSource) {

    public CarResponse publicView() {
        return toBuilder().vinNumber(null).build();
    }

    public CarResponse withConversion(CurrencyType currency, BigDecimal converted, BigDecimal rate, String source) {
        return toBuilder()
                .displayCurrency(currency)
                .convertedPrice(converted)
                .exchangeRate(rate)
                .rateSource(source)
                .build();
    }
}
