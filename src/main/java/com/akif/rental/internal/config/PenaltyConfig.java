package com.akif.rental.internal.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties(prefix = "penalty")
public record PenaltyConfig(
        @DefaultValue("60") @Min(0) @Max(120) int gracePeriodMinutes,
        @DefaultValue("0.10") @NotNull @DecimalMin("0.05") @DecimalMax("0.25") BigDecimal hourlyPenaltyRate,
        @DefaultValue("1.50") @NotNull @DecimalMin("1.00") @DecimalMax("2.00") BigDecimal dailyPenaltyRate,
        @DefaultValue("5.0") @NotNull @DecimalMin("3.0") @DecimalMax("10.0") BigDecimal penaltyCapMultiplier,
        @DefaultValue("24") @Min(1) int severelyLateThresholdHours) {
}
