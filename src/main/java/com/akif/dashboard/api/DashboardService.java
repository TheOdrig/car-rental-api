package com.akif.dashboard.api;

import com.akif.dashboard.api.dto.DailySummaryDto;
import com.akif.dashboard.api.dto.FleetStatusDto;
import com.akif.dashboard.api.dto.MonthlyMetricsDto;
import com.akif.dashboard.api.dto.PendingItemDto;
import com.akif.dashboard.api.dto.RevenueAnalyticsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface DashboardService {

    DailySummaryDto getDailySummary();

    FleetStatusDto getFleetStatus();

    MonthlyMetricsDto getMonthlyMetrics();

    MonthlyMetricsDto getMonthlyMetrics(LocalDate startDate, LocalDate endDate);

    RevenueAnalyticsDto getRevenueAnalytics();

    Page<PendingItemDto> getPendingApprovals(Pageable pageable);

    Page<PendingItemDto> getTodaysPickups(Pageable pageable);

    Page<PendingItemDto> getTodaysReturns(Pageable pageable);

    Page<PendingItemDto> getOverdueRentals(Pageable pageable);
}
