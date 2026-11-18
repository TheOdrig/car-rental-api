package com.akif.rental.api;

import java.time.LocalDateTime;

public record GracePeriodWarningEvent(
        Long rentalId,
        String customerEmail,
        LocalDateTime occurredAt,
        String carBrand,
        String carModel,
        String licensePlate,
        LocalDateTime scheduledReturnTime,
        int remainingGraceMinutes) implements RentalEvent {
}
