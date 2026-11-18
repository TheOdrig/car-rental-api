package com.akif.rental.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReturnReminderEvent(
        Long rentalId,
        String customerEmail,
        LocalDateTime occurredAt,
        LocalDate returnDate,
        CurrencyType currency,
        BigDecimal hourlyPenaltyAmount,
        BigDecimal dailyPenaltyAmount,
        int gracePeriodMinutes) implements RentalEvent {
}
