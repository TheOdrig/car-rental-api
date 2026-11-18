package com.akif.car.api;

import com.akif.car.api.CarStatusType;
import com.akif.shared.enums.CurrencyType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CarSummaryResponse(

    Long id,
    String licensePlate,
    String brand,
    String model,
    Integer productionYear,
    String formattedPrice,
    CurrencyType currencyType,
    CarStatusType carStatusType,
    String color,
    Long kilometer,
    String thumbnailUrl,
    BigDecimal rating,

    Integer age,
    String fullName,
    String displayName,
    Boolean isNew,
    Boolean isAvailable
) {}
