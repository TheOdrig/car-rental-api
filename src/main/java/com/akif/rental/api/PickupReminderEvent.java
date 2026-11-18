package com.akif.rental.api;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PickupReminderEvent(
        Long rentalId,
        String customerEmail,
        LocalDateTime occurredAt,
        LocalDate pickupDate,
        String carBrand,
        String carModel) implements RentalEvent {
}
