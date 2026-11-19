package com.akif.rental.internal.dto.report;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.util.List;

public record LateReturnStatisticsDto(
        Integer totalLateReturns,
        Integer severelyLateCount,
        List<CurrencyPenaltyTotals> penaltiesByCurrency,
        Double averageLateHours,
        Double lateReturnPercentage
) {

    public record CurrencyPenaltyTotals(
            CurrencyType currency,
            BigDecimal totalPenaltyAmount,
            BigDecimal collectedPenaltyAmount,
            BigDecimal pendingPenaltyAmount
    ) {
    }
}
