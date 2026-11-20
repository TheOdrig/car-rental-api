package com.akif.damage.internal.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "damage")
public record DamageConfig(
        @DefaultValue("500") @NotNull @DecimalMin("0.01") BigDecimal minorThreshold,
        @DefaultValue("2000") @NotNull @DecimalMin("0.01") BigDecimal moderateThreshold,
        @DefaultValue("10000") @NotNull @DecimalMin("0.01") BigDecimal majorThreshold,
        @DefaultValue("1000.00") @NotNull @DecimalMin("0.00") BigDecimal defaultInsuranceDeductible,
        @DefaultValue("10") @Min(1) @Max(50) int maxPhotosPerReport,
        @DefaultValue("10485760") @Min(1) long maxPhotoSizeBytes,
        @DefaultValue({"image/jpeg", "image/png", "image/heic"}) @NotEmpty List<String> allowedPhotoTypes,
        @DefaultValue("damage-photos") @NotBlank String photoStorageDirectory,
        @DefaultValue("60") @Min(1) int photoUrlExpirationMinutes,
        @DefaultValue("7") @Min(0) int reportWindowDaysAfterReturn) {

    @AssertTrue(message = "damage thresholds must be strictly increasing: minor < moderate < major")
    public boolean isThresholdsOrdered() {
        return minorThreshold == null || moderateThreshold == null || majorThreshold == null
                || (minorThreshold.compareTo(moderateThreshold) < 0
                        && moderateThreshold.compareTo(majorThreshold) < 0);
    }
}
