package com.akif.damage.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DamageChargeFailedEvent(
        Long damageReportId,
        Long rentalId,
        String customerEmail,
        BigDecimal amount,
        String failureReason,
        LocalDateTime failedAt) {
}
