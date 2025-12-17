package com.akif.dashboard.api.dto;

import java.time.LocalDateTime;

public record DailySummaryDto(
    int pendingApprovals,
    int todaysPickups,
    int todaysReturns,
    int overdueRentals,
    int pendingDamageAssessments,
    LocalDateTime generatedAt
) {}

