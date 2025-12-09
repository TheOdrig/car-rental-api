package com.akif.dto.penalty;

import com.akif.shared.enums.LateReturnStatus;

import java.math.BigDecimal;

public record PenaltyResult(
    BigDecimal penaltyAmount,
    BigDecimal dailyRate,
    int lateHours,
    int lateDays,
    LateReturnStatus status,
    String breakdown,
    boolean cappedAtMax
) {
}
