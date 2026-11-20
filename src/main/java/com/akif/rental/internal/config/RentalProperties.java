package com.akif.rental.internal.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "rental")
public class RentalProperties {

    private static final int STRIPE_HOLD_DAYS = 7;
    private static final int MAX_PAYMENT_GRACE_HOURS = 72;

    private int maxRentalDays = 30;
    private int maxLeadDays = 180;
    private int maxOpenRequestsPerUser = 3;
    private int requestExpiryHours = 24;
    private int paymentWindowDays = 5;
    private int paymentGraceHours = 12;
    private int noShowDays = 1;

    @PostConstruct
    public void validate() {
        if (paymentGraceHours < 1 || paymentGraceHours > MAX_PAYMENT_GRACE_HOURS) {
            int boundedHours = Math.min(Math.max(paymentGraceHours, 1), MAX_PAYMENT_GRACE_HOURS);
            log.warn("Invalid rental.payment-grace-hours: {}. It must be between 1 and {}. Using {}",
                    paymentGraceHours, MAX_PAYMENT_GRACE_HOURS, boundedHours);
            paymentGraceHours = boundedHours;
        }
        int latestSafeDay = Math.max(1, STRIPE_HOLD_DAYS - 1 - paymentWindowDays);
        if (noShowDays < 1 || noShowDays > latestSafeDay) {
            int clamped = Math.min(Math.max(noShowDays, 1), latestSafeDay);
            log.warn("Invalid rental.no-show-days: {}. It must be between 1 and {} so the hold is released before the {}-day authorisation expires. Using {}",
                    noShowDays, latestSafeDay, STRIPE_HOLD_DAYS, clamped);
            noShowDays = clamped;
        }
    }
}
