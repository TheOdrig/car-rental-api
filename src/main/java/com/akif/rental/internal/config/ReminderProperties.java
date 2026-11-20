package com.akif.rental.internal.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "rental.reminder")
public record ReminderProperties(
        @DefaultValue("50") @Min(1) @Max(1000) int batchSize,
        @DefaultValue("200") @Min(1) @Max(10000) int maxBatches) {
}
