package com.akif.event;

import com.akif.shared.enums.CurrencyType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class SeverelyLateNotificationEvent extends RentalEvent {
    
    private final String carBrand;
    private final String carModel;
    private final String licensePlate;
    private final LocalDateTime scheduledReturnTime;
    private final int lateHours;
    private final int lateDays;
    private final BigDecimal currentPenaltyAmount;
    private final CurrencyType currency;
    private final String escalationWarning;
    
    public SeverelyLateNotificationEvent(
            Object source,
            Long rentalId,
            String customerEmail,
            LocalDateTime occurredAt,
            String carBrand,
            String carModel,
            String licensePlate,
            LocalDateTime scheduledReturnTime,
            int lateHours,
            int lateDays,
            BigDecimal currentPenaltyAmount,
            CurrencyType currency,
            String escalationWarning) {
        super(source, rentalId, customerEmail, occurredAt);
        this.carBrand = carBrand;
        this.carModel = carModel;
        this.licensePlate = licensePlate;
        this.scheduledReturnTime = scheduledReturnTime;
        this.lateHours = lateHours;
        this.lateDays = lateDays;
        this.currentPenaltyAmount = currentPenaltyAmount;
        this.currency = currency;
        this.escalationWarning = escalationWarning;
    }
}
