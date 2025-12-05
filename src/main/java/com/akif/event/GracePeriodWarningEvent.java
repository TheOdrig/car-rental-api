package com.akif.event;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class GracePeriodWarningEvent extends RentalEvent {
    
    private final String carBrand;
    private final String carModel;
    private final String licensePlate;
    private final LocalDateTime scheduledReturnTime;
    private final int remainingGraceMinutes;
    
    public GracePeriodWarningEvent(
            Object source,
            Long rentalId,
            String customerEmail,
            LocalDateTime occurredAt,
            String carBrand,
            String carModel,
            String licensePlate,
            LocalDateTime scheduledReturnTime,
            int remainingGraceMinutes) {
        super(source, rentalId, customerEmail, occurredAt);
        this.carBrand = carBrand;
        this.carModel = carModel;
        this.licensePlate = licensePlate;
        this.scheduledReturnTime = scheduledReturnTime;
        this.remainingGraceMinutes = remainingGraceMinutes;
    }
}
