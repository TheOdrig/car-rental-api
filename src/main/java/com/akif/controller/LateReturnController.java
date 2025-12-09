package com.akif.controller;

import com.akif.dto.report.LateReturnFilterDto;
import com.akif.dto.report.LateReturnReportDto;
import com.akif.dto.report.LateReturnStatisticsDto;
import com.akif.service.report.ILateReturnReportService;
import com.akif.shared.enums.LateReturnStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/late-returns")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Late Return Management", description = "Admin operations for managing and reporting late returns")
public class LateReturnController {

    private final ILateReturnReportService lateReturnReportService;

    @GetMapping
    @Operation(summary = "Get late returns report", 
               description = "Retrieve all late returns with optional filtering and pagination (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Late returns retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin only")
    })
    public ResponseEntity<Page<LateReturnReportDto>> getLateReturns(
            @Parameter(description = "Start date for filtering (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for filtering (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Late return status filter")
            @RequestParam(required = false) String status,
            @Parameter(description = "Sort field (endDate, penaltyAmount, lateHours)")
            @RequestParam(required = false, defaultValue = "endDate") String sortBy,
            @Parameter(description = "Sort direction (ASC, DESC)")
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            @Parameter(description = "Pagination information")
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("GET /api/admin/late-returns - startDate: {}, endDate: {}, status: {}, sortBy: {}, sortDirection: {}",
                startDate, endDate, status, sortBy, sortDirection);

        LateReturnFilterDto filter = LateReturnFilterDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .status(status != null ? LateReturnStatus.valueOf(status) : null)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        Page<LateReturnReportDto> lateReturns = lateReturnReportService.getLateReturns(filter, pageable);

        log.info("Retrieved {} late returns", lateReturns.getTotalElements());
        return ResponseEntity.ok(lateReturns);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get late return statistics",
               description = "Retrieve aggregated statistics for late returns within a date range (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LateReturnStatisticsDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin only")
    })
    public ResponseEntity<LateReturnStatisticsDto> getStatistics(
            @Parameter(description = "Start date for statistics (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for statistics (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.debug("GET /api/admin/late-returns/statistics - startDate: {}, endDate: {}", startDate, endDate);

        LateReturnStatisticsDto statistics = lateReturnReportService.getStatistics(startDate, endDate);

        log.info("Retrieved late return statistics: totalLateReturns={}, totalPenaltyAmount={}",
                statistics.totalLateReturns(), statistics.totalPenaltyAmount());
        return ResponseEntity.ok(statistics);
    }
}
