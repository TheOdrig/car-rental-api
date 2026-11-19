package com.akif.rental.api;

import com.akif.shared.enums.CurrencyType;
import java.math.BigDecimal;
import java.time.Instant;

public record LateReturnNotificationEvent(
        Long rentalId,
        String customerEmail,
        Instant occurredAt,
        String carBrand,
        String carModel,
        String licensePlate,
        Instant scheduledReturnTime,
        int lateHours,
        BigDecimal currentPenaltyAmount,
        CurrencyType currency) implements RentalEvent {
}
