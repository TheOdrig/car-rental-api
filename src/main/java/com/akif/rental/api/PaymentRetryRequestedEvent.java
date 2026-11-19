package com.akif.rental.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

public record PaymentRetryRequestedEvent(
        Long rentalId,
        String customerEmail,
        Instant occurredAt,
        String carBrand,
        String carModel,
        LocalDate payBy,
        BigDecimal amount,
        CurrencyType currency) implements RentalEvent {
}
