package com.akif.damage.api;

import java.time.LocalDateTime;

public record DamageDisputedEvent(
        Long damageReportId,
        Long rentalId,
        String customerEmail,
        String disputeReason,
        LocalDateTime disputedAt) {
}
