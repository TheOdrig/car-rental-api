package com.akif.damage.internal.dto.damage.response;

import com.akif.damage.api.DamageStatus;
import com.akif.shared.enums.CurrencyType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DamageChargeResponse(
    Long damageId,
    DamageStatus status,
    @Schema(description = "True when the saved card was charged. A declined or undecided charge answers 200 with charged=false and the report stays ASSESSED so the charge can be retried.")
    boolean charged,
    BigDecimal amount,
    CurrencyType currency,
    String transactionId,
    String message
) {}
