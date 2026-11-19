package com.akif.rental.internal.dto.penalty;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PenaltyWaiverResponse(

    Long id,
    Long rentalId,
    BigDecimal originalPenalty,
    BigDecimal waivedAmount,
    BigDecimal remainingPenalty,
    String reason,
    Long adminId,

    Instant waivedAt,

    Boolean refundInitiated,
    String refundTransactionId
) {}
