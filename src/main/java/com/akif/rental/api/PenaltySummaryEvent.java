package com.akif.rental.api;

import com.akif.shared.enums.CurrencyType;
import java.math.BigDecimal;
import java.time.Instant;

public record PenaltySummaryEvent(
        Long rentalId,
        String customerEmail,
        Instant occurredAt,
        String carBrand,
        String carModel,
        String licensePlate,
        Instant scheduledReturnTime,
        Instant actualReturnTime,
        int lateHours,
        int lateDays,
        BigDecimal finalPenaltyAmount,
        CurrencyType currency,
        String penaltyBreakdown,
        boolean cappedAtMax) implements RentalEvent {
}
