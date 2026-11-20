package com.akif.car.internal.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "car.availability")
public record AvailabilityProperties(
        @DefaultValue("30") @Min(1) @Max(365) int maxSearchDays) {
}
