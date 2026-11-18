package com.akif.rental.api;

import com.akif.shared.enums.CurrencyType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LateReturnNotificationEvent(
        Long rentalId,
        String customerEmail,
        LocalDateTime occurredAt,
        String carBrand,
        String carModel,
        String licensePlate,
        LocalDateTime scheduledReturnTime,
        int lateHours,
        BigDecimal currentPenaltyAmount,
        CurrencyType currency) implements RentalEvent {
}
