package com.akif.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "damage")
@Getter
@Setter
public class DamageConfig {

    private BigDecimal minorThreshold = new BigDecimal("500");
    private BigDecimal moderateThreshold = new BigDecimal("2000");
    private BigDecimal majorThreshold = new BigDecimal("10000");

    private BigDecimal defaultInsuranceDeductible = new BigDecimal("1000.00");

    private int maxPhotosPerReport = 10;
    private long maxPhotoSizeBytes = 10485760L;
    private List<String> allowedPhotoTypes = List.of("image/jpeg", "image/png", "image/heic");

    private String photoStorageDirectory = "damage-photos";
    private int photoUrlExpirationMinutes = 60;
}
