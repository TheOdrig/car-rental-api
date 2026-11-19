package com.akif.damage.internal.dto.damage.response;

import com.akif.damage.api.DamageStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DamageDisputeResponse(
    
    Long damageId,
    DamageStatus status,
    String disputeReason,
    String disputeComments,
    String resolutionNotes,
    BigDecimal originalLiability,
    BigDecimal adjustedLiability,
    BigDecimal refundAmount,
    Instant disputedAt,
    Instant resolvedAt
) {}
