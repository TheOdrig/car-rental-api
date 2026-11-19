package com.akif.rental.api;

import java.time.Instant;

public record GracePeriodWarningEvent(
        Long rentalId,
        String customerEmail,
        Instant occurredAt,
        String carBrand,
        String carModel,
        String licensePlate,
        Instant scheduledReturnTime,
        int remainingGraceMinutes) implements RentalEvent {
}
