package com.akif.damage.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DamageResolvedEvent(
        Long damageReportId,
        Long rentalId,
        String customerEmail,
        BigDecimal adjustedLiability,
        BigDecimal refundAmount,
        String resolutionNotes,
        LocalDateTime resolvedAt) {
}
