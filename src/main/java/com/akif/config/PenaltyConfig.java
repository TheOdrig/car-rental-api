package com.akif.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Getter
@Setter
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "penalty")
public class PenaltyConfig {

    private int gracePeriodMinutes = 60;
    private BigDecimal hourlyPenaltyRate = new BigDecimal("0.10");
    private BigDecimal dailyPenaltyRate = new BigDecimal("1.50");
    private BigDecimal penaltyCapMultiplier = new BigDecimal("5.0");
    private int severelyLateThresholdHours = 24;

    @PostConstruct
    public void validate() {
        validateGracePeriod();
        validateHourlyPenaltyRate();
        validateDailyPenaltyRate();
        validatePenaltyCapMultiplier();
    }

    private void validateGracePeriod() {
        if (gracePeriodMinutes < 0 || gracePeriodMinutes > 120) {
            log.warn("Invalid grace period: {} minutes. Must be between 0 and 120. Using default: 60", 
                    gracePeriodMinutes);
            gracePeriodMinutes = 60;
        }
    }

    private void validateHourlyPenaltyRate() {
        BigDecimal min = new BigDecimal("0.05");
        BigDecimal max = new BigDecimal("0.25");
        if (hourlyPenaltyRate.compareTo(min) < 0 || hourlyPenaltyRate.compareTo(max) > 0) {
            log.warn("Invalid hourly penalty rate: {}. Must be between 0.05 and 0.25. Using default: 0.10", 
                    hourlyPenaltyRate);
            hourlyPenaltyRate = new BigDecimal("0.10");
        }
    }

    private void validateDailyPenaltyRate() {
        BigDecimal min = new BigDecimal("1.00");
        BigDecimal max = new BigDecimal("2.00");
        if (dailyPenaltyRate.compareTo(min) < 0 || dailyPenaltyRate.compareTo(max) > 0) {
            log.warn("Invalid daily penalty rate: {}. Must be between 1.00 and 2.00. Using default: 1.50", 
                    dailyPenaltyRate);
            dailyPenaltyRate = new BigDecimal("1.50");
        }
    }

    private void validatePenaltyCapMultiplier() {
        BigDecimal min = new BigDecimal("3.0");
        BigDecimal max = new BigDecimal("10.0");
        if (penaltyCapMultiplier.compareTo(min) < 0 || penaltyCapMultiplier.compareTo(max) > 0) {
            log.warn("Invalid penalty cap multiplier: {}. Must be between 3.0 and 10.0. Using default: 5.0", 
                    penaltyCapMultiplier);
            penaltyCapMultiplier = new BigDecimal("5.0");
        }
    }
}
