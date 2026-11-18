package com.akif.damage.api;

import java.time.LocalDateTime;

public record DamageReportedEvent(
        Long damageReportId,
        Long rentalId,
        Long carId,
        String carLicensePlate,
        String customerEmail,
        String customerFullName,
        String description,
        DamageSeverity severity,
        LocalDateTime reportedAt) {
}
