package com.akif.auth.api;

import java.math.BigDecimal;

public record UserDamageStatisticsDto(
        int totalDamageReports,
        BigDecimal totalDamageCost) {
}
