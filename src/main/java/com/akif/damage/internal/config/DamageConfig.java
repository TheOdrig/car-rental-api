package com.akif.damage.internal.config;

import com.akif.shared.enums.CurrencyType;
import jakarta.validation.Valid;
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
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "damage")
public record DamageConfig(
        @DefaultValue("500") @NotNull @DecimalMin("0.01") BigDecimal minorThreshold,
        @DefaultValue("2000") @NotNull @DecimalMin("0.01") BigDecimal moderateThreshold,
        @DefaultValue("10000") @NotNull @DecimalMin("0.01") BigDecimal majorThreshold,
        @DefaultValue("1000.00") @NotNull @DecimalMin("0.00") BigDecimal defaultInsuranceDeductible,
        Map<CurrencyType, @Valid @NotNull CurrencyLimits> currencyLimits,
        @DefaultValue("10") @Min(1) @Max(50) int maxPhotosPerReport,
        @DefaultValue("10485760") @Min(1) long maxPhotoSizeBytes,
        @DefaultValue({"image/jpeg", "image/png", "image/heic"}) @NotEmpty List<String> allowedPhotoTypes,
        @DefaultValue("damage-photos") @NotBlank String photoStorageDirectory,
        @DefaultValue("60") @Min(1) int photoUrlExpirationMinutes,
        @DefaultValue("7") @Min(0) int reportWindowDaysAfterReturn) {

    public DamageConfig {
        currencyLimits = currencyLimits == null ? Map.of() : Map.copyOf(currencyLimits);
    }

    public BigDecimal minorThresholdFor(CurrencyType currency) {
        CurrencyLimits limits = currencyLimits.get(currency);
        return limits != null && limits.minorThreshold() != null ? limits.minorThreshold() : minorThreshold;
    }

    public BigDecimal moderateThresholdFor(CurrencyType currency) {
        CurrencyLimits limits = currencyLimits.get(currency);
        return limits != null && limits.moderateThreshold() != null ? limits.moderateThreshold() : moderateThreshold;
    }

    public BigDecimal majorThresholdFor(CurrencyType currency) {
        CurrencyLimits limits = currencyLimits.get(currency);
        return limits != null && limits.majorThreshold() != null ? limits.majorThreshold() : majorThreshold;
    }

    public BigDecimal insuranceDeductibleFor(CurrencyType currency) {
        CurrencyLimits limits = currencyLimits.get(currency);
        return limits != null && limits.insuranceDeductible() != null
                ? limits.insuranceDeductible() : defaultInsuranceDeductible;
    }

    @AssertTrue(message = "damage thresholds must be strictly increasing: minor < moderate < major")
    public boolean isThresholdsOrdered() {
        return minorThreshold == null || moderateThreshold == null || majorThreshold == null
                || (minorThreshold.compareTo(moderateThreshold) < 0
                        && moderateThreshold.compareTo(majorThreshold) < 0);
    }

    @AssertTrue(message = "damage.currency-limits thresholds must be strictly increasing per currency")
    public boolean isCurrencyThresholdsOrdered() {
        return currencyLimits.keySet().stream().allMatch(currency ->
                minorThresholdFor(currency).compareTo(moderateThresholdFor(currency)) < 0
                        && moderateThresholdFor(currency).compareTo(majorThresholdFor(currency)) < 0);
    }

    public record CurrencyLimits(
            @DecimalMin("0.01") BigDecimal minorThreshold,
            @DecimalMin("0.01") BigDecimal moderateThreshold,
            @DecimalMin("0.01") BigDecimal majorThreshold,
            @DecimalMin("0.00") BigDecimal insuranceDeductible) {
    }
}
