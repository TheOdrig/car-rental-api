package com.akif.car.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;

public record CarDto(
    Long id,
    String brand,
    String model,
    String licensePlate,
    BigDecimal dailyPrice,
    CurrencyType currency,
    CarStatusType status,
    String bodyType,
    Integer seats,
    String thumbnailUrl,
    boolean available,
    boolean deleted
) {
    public boolean isAvailableForRental() {
        return available && !deleted && status == CarStatusType.AVAILABLE;
    }
}
