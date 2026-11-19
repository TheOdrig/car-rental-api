package com.akif.rental.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PenaltyChargeFailedEvent(
        Long rentalId,
        Long paymentId,
        String customerEmail,
        LocalDateTime occurredAt,
        BigDecimal amount,
        CurrencyType currency,
        String reason) implements RentalEvent {
}
