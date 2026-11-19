package com.akif.rental.web;

import com.akif.rental.internal.dto.report.LateReturnFilterDto;
import com.akif.rental.internal.dto.report.LateReturnReportDto;
import com.akif.rental.internal.dto.report.LateReturnStatisticsDto;
import com.akif.rental.internal.service.report.LateReturnReportService;
import com.akif.rental.domain.enums.LateReturnStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.akif.shared.web.PageResponse;
import org.springframework.data.domain.Page;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/late-returns")
@RequiredArgsConstructor
@Tag(name = "Late Return Management", description = "Admin operations for managing and reporting late returns")
public class LateReturnController {

    private final LateReturnReportService lateReturnReportService;

    @GetMapping
    @Operation(summary = "Get late returns report", 
               description = "Retrieve all late returns with optional filtering and pagination (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Late returns retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin only")
    })
    public ResponseEntity<PageResponse<LateReturnReportDto>> getLateReturns(
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
            @PageableDefault(size = 20) @ParameterObject Pageable pageable) {

        if (!java.util.Set.of("endDate", "penaltyAmount", "lateHours").contains(sortBy)) {
            throw new com.akif.shared.exception.InvalidParameterException(
                    "Cannot sort by '" + sortBy + "'. Supported sort fields: endDate, lateHours, penaltyAmount");
        }
        if (!"ASC".equalsIgnoreCase(sortDirection) && !"DESC".equalsIgnoreCase(sortDirection)) {
            throw new com.akif.shared.exception.InvalidParameterException("sortDirection must be ASC or DESC");
        }
        LateReturnStatus lateStatus = null;
        if (status != null) {
            try {
                lateStatus = LateReturnStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new com.akif.shared.exception.InvalidParameterException("Unknown late return status '" + status + "'");
            }
        }

        LateReturnFilterDto filter = LateReturnFilterDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .status(lateStatus)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        Page<LateReturnReportDto> lateReturns = lateReturnReportService.getLateReturns(filter, pageable);

        return ResponseEntity.ok(PageResponse.of(lateReturns));
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


        LateReturnStatisticsDto statistics = lateReturnReportService.getStatistics(startDate, endDate);

        return ResponseEntity.ok(statistics);
    }
}
