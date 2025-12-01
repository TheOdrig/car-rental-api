package com.akif.dto.reconciliation;

import java.math.BigDecimal;

public record StripePayment(
    String chargeId,
    String paymentIntentId,
    BigDecimal amount,
    String currency,
    String status
) {}
