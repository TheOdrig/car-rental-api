package com.akif.payment.internal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StripePayment(
    String chargeId,
    String paymentIntentId,
    BigDecimal amount,
    String currency,
    String status,
    LocalDate createdOn
) {}
