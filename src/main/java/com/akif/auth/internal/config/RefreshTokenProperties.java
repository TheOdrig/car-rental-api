package com.akif.auth.internal.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth.refresh-token")
public record RefreshTokenProperties(
        @DefaultValue("7") @Min(0) int retentionDays) {
}
