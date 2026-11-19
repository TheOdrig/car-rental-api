package com.akif.damage.web;

import com.akif.shared.web.AllowedSort;
import com.akif.damage.api.DamageReportDto;
import com.akif.damage.api.DamageCategory;
import com.akif.damage.api.DamageSeverity;
import com.akif.damage.api.DamageStatus;
import com.akif.damage.internal.service.damage.DamageHistoryService;
import com.akif.damage.internal.dto.damage.request.DamageSearchFilterDto;
import com.akif.damage.internal.dto.damage.response.DamageStatisticsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.akif.shared.security.AuthenticatedUser;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Tag(name = "Damage History", description = "Operations for damage history and statistics")
public class DamageHistoryController {

    private final DamageHistoryService damageHistoryService;

    @AllowedSort({"reportedAt", "createTime", "severity", "status"})
    @GetMapping("/api/v1/admin/damages/vehicle/{carId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get vehicle damage history", description = "Get damage history for a vehicle (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "History retrieved"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<PageResponse<DamageReportDto>> getVehicleDamageHistory(
            @Parameter(description = "Car ID", required = true)
            @PathVariable Long carId,
            @PageableDefault(size = 20) @ParameterObject Pageable pageable) {

        Page<DamageReportDto> damages = damageHistoryService.getDamagesByVehicle(carId, pageable);
        return ResponseEntity.ok(PageResponse.of(damages));
    }

    @AllowedSort({"reportedAt", "createTime", "severity", "status"})
    @GetMapping("/api/v1/admin/damages/customer/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get customer damage history", description = "Get damage history for a customer (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "History retrieved"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<PageResponse<DamageReportDto>> getCustomerDamageHistory(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,
            @PageableDefault(size = 20) @ParameterObject Pageable pageable) {

        Page<DamageReportDto> damages = damageHistoryService.getDamagesByCustomer(userId, pageable);
        return ResponseEntity.ok(PageResponse.of(damages));
    }

    @AllowedSort({"reportedAt", "createTime", "severity", "status"})
    @GetMapping("/api/v1/damages/me")
    @Operation(summary = "Get my damage history", description = "Get current user's damage history")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "History retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PageResponse<DamageReportDto>> getMyDamageHistory(
            @PageableDefault(size = 20) @ParameterObject Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser caller) {

        Page<DamageReportDto> damages = damageHistoryService.getDamagesByCustomer(caller.id(), pageable);
        return ResponseEntity.ok(PageResponse.of(damages));
    }

    @AllowedSort({"reportedAt", "createTime", "severity", "status"})
    @GetMapping("/api/v1/admin/damages/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search damages", description = "Search damages with filters (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<PageResponse<DamageReportDto>> searchDamages(
            @Parameter(description = "Start date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Severity filter")
            @RequestParam(required = false) DamageSeverity severity,
            @Parameter(description = "Category filter")
            @RequestParam(required = false) DamageCategory category,
            @Parameter(description = "Status filter")
            @RequestParam(required = false) DamageStatus status,
            @Parameter(description = "Car ID filter")
            @RequestParam(required = false) Long carId,
            @PageableDefault(size = 20) @ParameterObject Pageable pageable) {

        DamageSearchFilterDto filter = new DamageSearchFilterDto(
                startDate, endDate, severity, category, status, carId
        );
        Page<DamageReportDto> damages = damageHistoryService.searchDamages(filter, pageable);
        return ResponseEntity.ok(PageResponse.of(damages));
    }

    @GetMapping("/api/v1/admin/damages/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get damage statistics", description = "Get damage statistics for date range (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<DamageStatisticsDto> getDamageStatistics(
            @Parameter(description = "Start date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        DamageStatisticsDto statistics = damageHistoryService.getDamageStatistics(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }
}
