package com.akif.damage.api;

import java.time.Instant;

public record DamageDisputedEvent(
        Long damageReportId,
        Long rentalId,
        String customerEmail,
        String disputeReason,
        Instant disputedAt) {
}
