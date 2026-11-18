package com.akif.rental.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentRetryRequestedEvent(
        Long rentalId,
        String customerEmail,
        LocalDateTime occurredAt,
        String carBrand,
        String carModel,
        LocalDate payBy,
        BigDecimal amount,
        CurrencyType currency) implements RentalEvent {
}
