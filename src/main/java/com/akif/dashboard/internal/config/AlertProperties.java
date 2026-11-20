package com.akif.dashboard.internal.config;

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
@ConfigurationProperties(prefix = "dashboard.alerts")
public record AlertProperties(
        @DefaultValue("20.00") @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal lowAvailabilityPercent,
        @DefaultValue("7") @Min(1) int disputeDaysThreshold,
        @DefaultValue("100") @Min(1) @Max(1000) int overduePageSize) {
}
