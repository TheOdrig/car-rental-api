package com.akif.dto.reconciliation;

import com.akif.enums.DiscrepancyType;

import java.math.BigDecimal;

public record Discrepancy(
    DiscrepancyType type,
    String paymentId,
    String stripePaymentIntentId,
    BigDecimal databaseAmount,
    BigDecimal stripeAmount,
    String databaseStatus,
    String stripeStatus,
    String description
) {}
