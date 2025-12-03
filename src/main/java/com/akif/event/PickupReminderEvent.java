package com.akif.event;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class PickupReminderEvent extends RentalEvent {
    
    private final LocalDate pickupDate;
    private final String pickupLocation;
    private final String carBrand;
    private final String carModel;
    
    public PickupReminderEvent(
            Object source,
            Long rentalId,
            String customerEmail,
            LocalDateTime occurredAt,
            LocalDate pickupDate,
            String pickupLocation,
            String carBrand,
            String carModel) {
        super(source, rentalId, customerEmail, occurredAt);
        this.pickupDate = pickupDate;
        this.pickupLocation = pickupLocation;
        this.carBrand = carBrand;
        this.carModel = carModel;
    }
}
