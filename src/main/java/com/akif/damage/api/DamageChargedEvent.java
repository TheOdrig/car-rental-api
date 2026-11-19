package com.akif.damage.api;

import java.math.BigDecimal;
import java.time.Instant;

public record DamageChargedEvent(
        Long damageReportId,
        Long rentalId,
        String customerEmail,
        BigDecimal chargeAmount,
        String transactionId,
        Instant chargedAt) {
}
