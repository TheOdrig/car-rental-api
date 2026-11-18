package com.akif.rental.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReturnReminderEvent(
        Long rentalId,
        String customerEmail,
        LocalDateTime occurredAt,
        LocalDate returnDate,
        String returnLocation,
        BigDecimal dailyPenaltyRate) implements RentalEvent {
}
