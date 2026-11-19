package com.akif.rental.api;

import java.time.LocalDate;
import java.time.Instant;

public record PickupReminderEvent(
        Long rentalId,
        String customerEmail,
        Instant occurredAt,
        LocalDate pickupDate,
        String carBrand,
        String carModel) implements RentalEvent {
}
