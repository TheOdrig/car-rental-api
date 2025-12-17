package com.akif.dashboard.internal.service;

import com.akif.dashboard.api.DashboardService;
import com.akif.dashboard.api.dto.DailySummaryDto;
import com.akif.dashboard.api.dto.FleetStatusDto;
import com.akif.dashboard.api.dto.MonthlyMetricsDto;
import com.akif.dashboard.api.dto.PendingItemDto;
import com.akif.dashboard.api.dto.RevenueAnalyticsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final DashboardQueryService queryService;

    @Override
    @Cacheable(value = "dailySummary", key = "'current'")
    public DailySummaryDto getDailySummary() {
        log.info("Fetching daily summary (cache miss)");
        DailySummaryDto summary = queryService.fetchDailySummary();
        log.info("Daily summary: pending={}, pickups={}, returns={}, overdue={}, damage={}",
            summary.pendingApprovals(), summary.todaysPickups(), summary.todaysReturns(),
            summary.overdueRentals(), summary.pendingDamageAssessments());
        return summary;
    }

    @Override
    @Cacheable(value = "fleetStatus", key = "'current'")
    public FleetStatusDto getFleetStatus() {
        log.info("Fetching fleet status (cache miss)");
        FleetStatusDto status = queryService.fetchFleetStatus();
        log.info("Fleet status: total={}, available={}, rented={}, occupancy={}%",
            status.totalCars(), status.availableCars(), status.rentedCars(), status.occupancyRate());
        return status;
    }

    @Override
    @Cacheable(value = "monthlyMetrics", key = "'current'")
    public MonthlyMetricsDto getMonthlyMetrics() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.withDayOfMonth(1);
        return getMonthlyMetrics(startDate, endDate);
    }

    @Override
    @Cacheable(value = "monthlyMetrics", key = "#startDate.toString() + '-' + #endDate.toString()")
    public MonthlyMetricsDto getMonthlyMetrics(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching monthly metrics (cache miss) for: {} to {}", startDate, endDate);
        MonthlyMetricsDto metrics = queryService.fetchMonthlyMetrics(startDate, endDate);
        log.info("Monthly metrics: revenue={}, completed={}, cancelled={}",
            metrics.totalRevenue(), metrics.completedRentals(), metrics.cancelledRentals());
        return metrics;
    }

    @Override
    @Cacheable(value = "revenueAnalytics", key = "'current'")
    public RevenueAnalyticsDto getRevenueAnalytics() {
        log.info("Fetching revenue analytics (cache miss)");
        RevenueAnalyticsDto analytics = queryService.fetchRevenueAnalytics();
        log.info("Revenue analytics: {} daily records, {} monthly records",
            analytics.dailyRevenue().size(), analytics.monthlyRevenue().size());
        return analytics;
    }

    @Override
    public Page<PendingItemDto> getPendingApprovals(Pageable pageable) {
        log.debug("Getting pending approvals (real-time, no cache)");
        return queryService.fetchPendingApprovals(pageable);
    }

    @Override
    public Page<PendingItemDto> getTodaysPickups(Pageable pageable) {
        log.debug("Getting today's pickups (real-time, no cache)");
        return queryService.fetchTodaysPickups(pageable);
    }

    @Override
    public Page<PendingItemDto> getTodaysReturns(Pageable pageable) {
        log.debug("Getting today's returns (real-time, no cache)");
        return queryService.fetchTodaysReturns(pageable);
    }

    @Override
    public Page<PendingItemDto> getOverdueRentals(Pageable pageable) {
        log.debug("Getting overdue rentals (real-time, no cache)");
        return queryService.fetchOverdueRentals(pageable);
    }
}
