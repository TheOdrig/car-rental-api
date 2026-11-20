package com.akif.rental.internal.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "rental")
public record RentalProperties(
        @DefaultValue("30") @Min(1) @Max(365) int maxRentalDays,
        @DefaultValue("180") @Min(0) @Max(730) int maxLeadDays,
        @DefaultValue("3") @Min(1) @Max(100) int maxOpenRequestsPerUser,
        @DefaultValue("24") @Min(1) @Max(720) int requestExpiryHours,
        @DefaultValue("5") @Min(1) @Max(6) int paymentWindowDays,
        @DefaultValue("12") @Min(1) @Max(72) int paymentGraceHours,
        @DefaultValue("1") @Min(1) int noShowDays) {

    private static final int STRIPE_HOLD_DAYS = 7;

    @AssertTrue(message = "rental.no-show-days must release the hold before the 7-day authorisation expires")
    public boolean isNoShowWithinAuthorisationWindow() {
        return noShowDays <= Math.max(1, STRIPE_HOLD_DAYS - 1 - paymentWindowDays);
    }
}
