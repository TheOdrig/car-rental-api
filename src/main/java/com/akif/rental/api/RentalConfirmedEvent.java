package com.akif.rental.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record RentalConfirmedEvent(
        Long rentalId,
        String customerEmail,
        LocalDateTime occurredAt,
        String carBrand,
        String carModel,
        LocalDate pickupDate,
        LocalDate returnDate,
        BigDecimal totalPrice,
        CurrencyType currency,
        String notes) implements RentalEvent {

    public RentalConfirmedEvent(
            Long rentalId,
            String customerEmail,
            LocalDateTime occurredAt,
            String carBrand,
            String carModel,
            LocalDate pickupDate,
            LocalDate returnDate,
            BigDecimal totalPrice,
            CurrencyType currency) {
        this(rentalId, customerEmail, occurredAt, carBrand, carModel, pickupDate, returnDate, totalPrice, currency,
                null);
    }
}
