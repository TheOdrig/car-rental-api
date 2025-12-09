package com.akif.event;

import com.akif.shared.enums.CurrencyType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class LateReturnNotificationEvent extends RentalEvent {
    
    private final String carBrand;
    private final String carModel;
    private final String licensePlate;
    private final LocalDateTime scheduledReturnTime;
    private final int lateHours;
    private final BigDecimal currentPenaltyAmount;
    private final CurrencyType currency;
    
    public LateReturnNotificationEvent(
            Object source,
            Long rentalId,
            String customerEmail,
            LocalDateTime occurredAt,
            String carBrand,
            String carModel,
            String licensePlate,
            LocalDateTime scheduledReturnTime,
            int lateHours,
            BigDecimal currentPenaltyAmount,
            CurrencyType currency) {
        super(source, rentalId, customerEmail, occurredAt);
        this.carBrand = carBrand;
        this.carModel = carModel;
        this.licensePlate = licensePlate;
        this.scheduledReturnTime = scheduledReturnTime;
        this.lateHours = lateHours;
        this.currentPenaltyAmount = currentPenaltyAmount;
        this.currency = currency;
    }
}
