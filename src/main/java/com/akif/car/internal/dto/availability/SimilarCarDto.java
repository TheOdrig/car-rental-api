package com.akif.car.internal.dto.availability;

import com.akif.shared.enums.CurrencyType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SimilarCarDto(

    Long id,
    String brand,
    String model,
    Integer productionYear,
    String bodyType,
    @Schema(description = "Effective price per day in the car currency. For a request without dates this is the price of a single day starting today. For a request with dates it is the total price divided by the number of rental days in that range.")
    BigDecimal dailyRate,
    @Schema(description = "Total price in the car currency for the priced range: one day when the request has no dates, otherwise the whole requested range.")
    BigDecimal totalPrice,
    CurrencyType currency,
    String imageUrl,
    List<String> similarityReasons,
    Integer similarityScore
) {}
