package com.akif.dashboard.api.dto;

import java.time.Instant;
import java.util.List;

public record RevenueAnalyticsDto(
    List<DailyRevenueDto> dailyRevenue,
    List<MonthlyRevenueDto> monthlyRevenue,
    List<RevenueBreakdownDto> breakdownByCurrency,
    Instant generatedAt
) {}
