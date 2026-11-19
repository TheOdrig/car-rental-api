package com.akif.damage.api;

import java.math.BigDecimal;
import java.time.Instant;

public record DamageChargeFailedEvent(
        Long damageReportId,
        Long rentalId,
        String customerEmail,
        BigDecimal amount,
        String failureReason,
        Instant failedAt) {
}
