package com.akif.rental.api;

import com.akif.shared.enums.CurrencyType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SeverelyLateNotificationEvent(
        Long rentalId,
        String customerEmail,
        LocalDateTime occurredAt,
        String carBrand,
        String carModel,
        String licensePlate,
        LocalDateTime scheduledReturnTime,
        int lateHours,
        int lateDays,
        BigDecimal currentPenaltyAmount,
        CurrencyType currency,
        String escalationWarning) implements RentalEvent {
}
