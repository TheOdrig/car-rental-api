package com.akif.auth.internal.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "auth.avatar")
public record AvatarProperties(
        @DefaultValue("5242880") @Min(1) long maxFileSizeBytes,
        @DefaultValue({"image/jpeg", "image/png", "image/gif", "image/webp"}) @NotEmpty List<String> allowedTypes,
        @DefaultValue("60") @Min(1) int defaultUrlExpirationMinutes) {
}
