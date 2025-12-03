package com.akif.event;

import com.akif.enums.CurrencyType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class RentalConfirmedEvent extends RentalEvent {
    
    private final String carBrand;
    private final String carModel;
    private final LocalDate pickupDate;
    private final LocalDate returnDate;
    private final BigDecimal totalPrice;
    private final CurrencyType currency;
    private final String pickupLocation;
    
    public RentalConfirmedEvent(
            Object source,
            Long rentalId,
            String customerEmail,
            LocalDateTime occurredAt,
            String carBrand,
            String carModel,
            LocalDate pickupDate,
            LocalDate returnDate,
            BigDecimal totalPrice,
            CurrencyType currency,
            String pickupLocation) {
        super(source, rentalId, customerEmail, occurredAt);
        this.carBrand = carBrand;
        this.carModel = carModel;
        this.pickupDate = pickupDate;
        this.returnDate = returnDate;
        this.totalPrice = totalPrice;
        this.currency = currency;
        this.pickupLocation = pickupLocation;
    }
}
