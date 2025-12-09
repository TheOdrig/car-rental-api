package com.akif.event;

import com.akif.shared.enums.CurrencyType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class PenaltySummaryEvent extends RentalEvent {
    
    private final String carBrand;
    private final String carModel;
    private final String licensePlate;
    private final LocalDateTime scheduledReturnTime;
    private final LocalDateTime actualReturnTime;
    private final int lateHours;
    private final int lateDays;
    private final BigDecimal finalPenaltyAmount;
    private final CurrencyType currency;
    private final String penaltyBreakdown;
    private final boolean cappedAtMax;
    
    public PenaltySummaryEvent(
            Object source,
            Long rentalId,
            String customerEmail,
            LocalDateTime occurredAt,
            String carBrand,
            String carModel,
            String licensePlate,
            LocalDateTime scheduledReturnTime,
            LocalDateTime actualReturnTime,
            int lateHours,
            int lateDays,
            BigDecimal finalPenaltyAmount,
            CurrencyType currency,
            String penaltyBreakdown,
            boolean cappedAtMax) {
        super(source, rentalId, customerEmail, occurredAt);
        this.carBrand = carBrand;
        this.carModel = carModel;
        this.licensePlate = licensePlate;
        this.scheduledReturnTime = scheduledReturnTime;
        this.actualReturnTime = actualReturnTime;
        this.lateHours = lateHours;
        this.lateDays = lateDays;
        this.finalPenaltyAmount = finalPenaltyAmount;
        this.currency = currency;
        this.penaltyBreakdown = penaltyBreakdown;
        this.cappedAtMax = cappedAtMax;
    }
}
