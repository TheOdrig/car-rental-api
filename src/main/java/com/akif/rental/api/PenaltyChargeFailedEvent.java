package com.akif.rental.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;

public record PenaltyChargeFailedEvent(
        Long rentalId,
        Long paymentId,
        String customerEmail,
        Instant occurredAt,
        BigDecimal amount,
        CurrencyType currency,
        String reason) implements RentalEvent {
}
