package com.akif.rental.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

public record ReturnReminderEvent(
        Long rentalId,
        String customerEmail,
        Instant occurredAt,
        LocalDate returnDate,
        CurrencyType currency,
        BigDecimal hourlyPenaltyAmount,
        BigDecimal dailyPenaltyAmount,
        int gracePeriodMinutes) implements RentalEvent {
}
