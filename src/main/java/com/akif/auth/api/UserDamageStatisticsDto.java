package com.akif.damage.api;

import java.math.BigDecimal;

public record UserDamageStatisticsDto(
        int totalDamageReports,
        BigDecimal totalDamageCost) {
}
