package com.akif.rental.api;

import com.akif.shared.enums.CurrencyType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PenaltySummaryEvent(
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
        boolean cappedAtMax) implements RentalEvent {
}
