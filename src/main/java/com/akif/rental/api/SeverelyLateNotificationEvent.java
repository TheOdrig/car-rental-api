package com.akif.rental.api;

import com.akif.shared.enums.CurrencyType;
import java.math.BigDecimal;
import java.time.Instant;

public record SeverelyLateNotificationEvent(
        Long rentalId,
        String customerEmail,
        Instant occurredAt,
        String carBrand,
        String carModel,
        String licensePlate,
        Instant scheduledReturnTime,
        int lateHours,
        int lateDays,
        BigDecimal currentPenaltyAmount,
        CurrencyType currency,
        String escalationWarning) implements RentalEvent {
}
