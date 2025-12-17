package com.akif.dashboard.web;

import com.akif.dashboard.api.DashboardService;
import com.akif.dashboard.api.dto.DailySummaryDto;
import com.akif.dashboard.api.dto.FleetStatusDto;
import com.akif.dashboard.api.dto.MonthlyMetricsDto;
import com.akif.dashboard.api.dto.PendingItemDto;
import com.akif.dashboard.api.dto.RevenueAnalyticsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Dashboard", description = "Dashboard operations for administrators")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get daily summary", description = "Returns pending approvals, pickups, returns, overdue rentals, and pending damage assessments")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Daily summary retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<DailySummaryDto> getDailySummary() {
        log.info("GET /api/admin/dashboard/summary");
        DailySummaryDto summary = dashboardService.getDailySummary();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/fleet")
    @Operation(summary = "Get fleet status", description = "Returns car availability statistics and occupancy rate")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fleet status retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<FleetStatusDto> getFleetStatus() {
        log.info("GET /api/admin/dashboard/fleet");
        FleetStatusDto fleetStatus = dashboardService.getFleetStatus();
        return ResponseEntity.ok(fleetStatus);
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get monthly metrics", description = "Returns revenue, completed/cancelled rentals, penalty revenue, damage charges for current month or specified date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Monthly metrics retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid date range"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<MonthlyMetricsDto> getMonthlyMetrics(
            @Parameter(description = "Start date (inclusive)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (inclusive)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("GET /api/admin/dashboard/metrics - startDate: {}, endDate: {}", startDate, endDate);
        
        MonthlyMetricsDto metrics;
        if (startDate != null && endDate != null) {
            metrics = dashboardService.getMonthlyMetrics(startDate, endDate);
        } else {
            metrics = dashboardService.getMonthlyMetrics();
        }
        
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/revenue")
    @Operation(summary = "Get revenue analytics", description = "Returns daily revenue, monthly revenue, and revenue breakdown")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Revenue analytics retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<RevenueAnalyticsDto> getRevenueAnalytics() {
        log.info("GET /api/admin/dashboard/revenue");
        RevenueAnalyticsDto analytics = dashboardService.getRevenueAnalytics();
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/pending/approvals")
    @Operation(summary = "Get pending approvals", description = "Returns paginated list of rentals awaiting approval")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pending approvals retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<Page<PendingItemDto>> getPendingApprovals(
            @PageableDefault(size = 10, sort = "createTime") Pageable pageable) {
        log.info("GET /api/admin/dashboard/pending/approvals - page: {}, size: {}", 
                pageable.getPageNumber(), pageable.getPageSize());
        Page<PendingItemDto> approvals = dashboardService.getPendingApprovals(pageable);
        return ResponseEntity.ok(approvals);
    }

    @GetMapping("/pending/pickups")
    @Operation(summary = "Get today's pickups", description = "Returns paginated list of confirmed rentals with today's start date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Today's pickups retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<Page<PendingItemDto>> getTodaysPickups(
            @PageableDefault(size = 10, sort = "startDate") Pageable pageable) {
        log.info("GET /api/admin/dashboard/pending/pickups - page: {}, size: {}", 
                pageable.getPageNumber(), pageable.getPageSize());
        Page<PendingItemDto> pickups = dashboardService.getTodaysPickups(pageable);
        return ResponseEntity.ok(pickups);
    }

    @GetMapping("/pending/returns")
    @Operation(summary = "Get today's returns", description = "Returns paginated list of in-use rentals with today's end date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Today's returns retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<Page<PendingItemDto>> getTodaysReturns(
            @PageableDefault(size = 10, sort = "endDate") Pageable pageable) {
        log.info("GET /api/admin/dashboard/pending/returns - page: {}, size: {}", 
                pageable.getPageNumber(), pageable.getPageSize());
        Page<PendingItemDto> returns = dashboardService.getTodaysReturns(pageable);
        return ResponseEntity.ok(returns);
    }

    @GetMapping("/pending/overdue")
    @Operation(summary = "Get overdue rentals", description = "Returns paginated list of rentals past their end date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Overdue rentals retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<Page<PendingItemDto>> getOverdueRentals(
            @PageableDefault(size = 10, sort = "endDate") Pageable pageable) {
        log.info("GET /api/admin/dashboard/pending/overdue - page: {}, size: {}", 
                pageable.getPageNumber(), pageable.getPageSize());
        Page<PendingItemDto> overdue = dashboardService.getOverdueRentals(pageable);
        return ResponseEntity.ok(overdue);
    }
}
