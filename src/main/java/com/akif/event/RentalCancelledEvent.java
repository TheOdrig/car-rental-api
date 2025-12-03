package com.akif.event;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class RentalCancelledEvent extends RentalEvent {
    
    private final LocalDateTime cancellationDate;
    private final String cancellationReason;
    private final boolean refundProcessed;
    private final BigDecimal refundAmount;
    private final String refundTransactionId;
    
    public RentalCancelledEvent(
            Object source,
            Long rentalId,
            String customerEmail,
            LocalDateTime occurredAt,
            LocalDateTime cancellationDate,
            String cancellationReason,
            boolean refundProcessed,
            BigDecimal refundAmount,
            String refundTransactionId) {
        super(source, rentalId, customerEmail, occurredAt);
        this.cancellationDate = cancellationDate;
        this.cancellationReason = cancellationReason;
        this.refundProcessed = refundProcessed;
        this.refundAmount = refundAmount;
        this.refundTransactionId = refundTransactionId;
    }
}
