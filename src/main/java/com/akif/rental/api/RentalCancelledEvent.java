package com.akif.rental.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RentalCancelledEvent(
        Long rentalId,
        String customerEmail,
        LocalDateTime occurredAt,
        LocalDateTime cancellationDate,
        String cancellationReason,
        boolean refundProcessed,
        BigDecimal refundAmount,
        String refundTransactionId) implements RentalEvent {
}
