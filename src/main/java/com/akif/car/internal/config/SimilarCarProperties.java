package com.akif.car.internal.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties(prefix = "car.similar")
public record SimilarCarProperties(
        @DefaultValue("0.20") @DecimalMin("0.00") @DecimalMax("1.00") BigDecimal priceBand,
        @DefaultValue("3") @Min(1) @Max(10) int candidateMultiplier,
        @DefaultValue("5") @Min(1) @Max(10) int defaultLimit,
        @DefaultValue("30") @Min(1) @Max(365) int defaultWindowDays,
        @DefaultValue("50") @Min(0) @Max(100) int bodyTypeWeight,
        @DefaultValue("30") @Min(0) @Max(100) int brandWeight,
        @DefaultValue("20") @Min(0) @Max(100) int priceWeight) {
}
