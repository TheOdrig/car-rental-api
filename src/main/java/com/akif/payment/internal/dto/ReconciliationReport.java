package com.akif.payment.internal.dto;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

public record ReconciliationReport(
    LocalDate reportDate,
    int totalDatabasePayments,
    int totalStripePayments,
    List<Discrepancy> discrepancies,
    boolean hasDiscrepancies,
    Instant generatedAt
) {}
