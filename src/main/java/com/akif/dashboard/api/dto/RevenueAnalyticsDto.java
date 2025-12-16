package com.akif.dashboard.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RevenueAnalyticsDto(
    List<DailyRevenueDto> dailyRevenue,
    List<MonthlyRevenueDto> monthlyRevenue,
    RevenueBreakdownDto breakdown,
    LocalDateTime generatedAt
) {}

