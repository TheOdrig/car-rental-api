package com.akif.damage.api;

import java.time.Instant;

public record DamageReportedEvent(
        Long damageReportId,
        Long rentalId,
        Long carId,
        String carLicensePlate,
        String customerEmail,
        String customerFullName,
        String description,
        DamageSeverity severity,
        Instant reportedAt) {
}
