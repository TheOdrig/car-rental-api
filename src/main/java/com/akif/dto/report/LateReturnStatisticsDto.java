package com.akif.dto.report;

import java.io.Serializable;
import java.math.BigDecimal;

public record LateReturnStatisticsDto(
        Integer totalLateReturns,
        Integer severelyLateCount,
        BigDecimal totalPenaltyAmount,
        BigDecimal collectedPenaltyAmount,
        BigDecimal pendingPenaltyAmount,
        Double averageLateHours,
        Double lateReturnPercentage
) implements Serializable {
}
