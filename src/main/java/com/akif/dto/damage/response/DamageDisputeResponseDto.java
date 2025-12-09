package com.akif.dto.damage.response;

import com.akif.shared.enums.DamageStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DamageDisputeResponseDto(
    
    Long damageId,
    DamageStatus status,
    String disputeReason,
    String disputeComments,
    String resolutionNotes,
    BigDecimal originalLiability,
    BigDecimal adjustedLiability,
    BigDecimal refundAmount,
    LocalDateTime disputedAt,
    LocalDateTime resolvedAt
) {}
