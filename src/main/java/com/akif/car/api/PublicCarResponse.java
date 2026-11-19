package com.akif.car.api;

import com.akif.shared.enums.CurrencyType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublicCarResponse(
        Long id,
        String licensePlate,
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
        Instant createTime,
        Instant updateTime,
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

    public static PublicCarResponse from(CarResponse car) {
        return new PublicCarResponse(
                car.id(),
                car.licensePlate(),
                car.brand(),
                car.model(),
                car.productionYear(),
                car.price(),
                car.currencyType(),
                car.carStatusType(),
                car.engineType(),
                car.engineDisplacement(),
                car.fuelType(),
                car.transmissionType(),
                car.bodyType(),
                car.color(),
                car.kilometer(),
                car.doors(),
                car.seats(),
                car.registrationDate(),
                car.lastServiceDate(),
                car.nextServiceDate(),
                car.insuranceExpiryDate(),
                car.inspectionExpiryDate(),
                car.notes(),
                car.imageUrl(),
                car.thumbnailUrl(),
                car.rating(),
                car.createTime(),
                car.updateTime(),
                car.version(),
                car.formattedPrice(),
                car.formattedPriceWithSeparators(),
                car.age(),
                car.fullName(),
                car.displayName(),
                car.convertedPrice(),
                car.displayCurrency(),
                car.exchangeRate(),
                car.rateSource());
    }

    public PublicCarResponse withConversion(CurrencyType currency, BigDecimal converted, BigDecimal rate, String source) {
        return toBuilder()
                .displayCurrency(currency)
                .convertedPrice(converted)
                .exchangeRate(rate)
                .rateSource(source)
                .build();
    }
}
