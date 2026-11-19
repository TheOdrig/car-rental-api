package com.akif.dashboard.api.dto;

import java.time.Instant;

public record DailySummaryDto(
    int pendingApprovals,
    int todaysPickups,
    int todaysReturns,
    int overdueRentals,
    int pendingDamageAssessments,
    Instant generatedAt
) {}

