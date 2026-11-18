package com.akif.rental.internal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "rental")
public class RentalProperties {

    private int maxRentalDays = 30;
    private int maxLeadDays = 180;
    private int maxOpenRequestsPerUser = 3;
    private int requestExpiryHours = 24;
}
