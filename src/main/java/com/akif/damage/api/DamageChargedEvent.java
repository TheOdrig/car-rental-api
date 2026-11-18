package com.akif.damage.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DamageChargedEvent(
        Long damageReportId,
        Long rentalId,
        String customerEmail,
        BigDecimal chargeAmount,
        String transactionId,
        LocalDateTime chargedAt) {
}
