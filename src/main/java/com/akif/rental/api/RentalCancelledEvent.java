package com.akif.rental.api;

import java.math.BigDecimal;
import java.time.Instant;

public record RentalCancelledEvent(
        Long rentalId,
        String customerEmail,
        Instant occurredAt,
        Instant cancellationDate,
        String cancellationReason,
        boolean refundProcessed,
        BigDecimal refundAmount,
        String refundTransactionId) implements RentalEvent {
}
