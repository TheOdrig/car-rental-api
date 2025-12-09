package com.akif.controller;

import com.akif.dto.damage.request.DamageSearchFilterDto;
import com.akif.dto.damage.response.DamageReportDto;
import com.akif.dto.damage.response.DamageStatisticsDto;
import com.akif.shared.enums.DamageCategory;
import com.akif.shared.enums.DamageSeverity;
import com.akif.shared.enums.DamageStatus;
import com.akif.auth.domain.User;
import com.akif.auth.repository.UserRepository;
import com.akif.service.damage.IDamageHistoryService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Damage History", description = "Operations for damage history and statistics")
public class DamageHistoryController {

    private final IDamageHistoryService damageHistoryService;
    private final UserRepository userRepository;

    @GetMapping("/api/admin/damages/vehicle/{carId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get vehicle damage history", description = "Get damage history for a vehicle (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "History retrieved"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Page<DamageReportDto>> getVehicleDamageHistory(
            @Parameter(description = "Car ID", required = true)
            @PathVariable Long carId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("GET /api/admin/damages/vehicle/{}", carId);
        Page<DamageReportDto> damages = damageHistoryService.getDamagesByVehicle(carId, pageable);
        return ResponseEntity.ok(damages);
    }

    @GetMapping("/api/admin/damages/customer/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get customer damage history", description = "Get damage history for a customer (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "History retrieved"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Page<DamageReportDto>> getCustomerDamageHistory(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("GET /api/admin/damages/customer/{}", userId);
        Page<DamageReportDto> damages = damageHistoryService.getDamagesByCustomer(userId, pageable);
        return ResponseEntity.ok(damages);
    }

    @GetMapping("/api/damages/me")
    @Operation(summary = "Get my damage history", description = "Get current user's damage history")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "History retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<DamageReportDto>> getMyDamageHistory(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("GET /api/damages/me - User: {}", username);
        
        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Page<DamageReportDto> damages = damageHistoryService.getDamagesByCustomer(user.getId(), pageable);
        return ResponseEntity.ok(damages);
    }

    @GetMapping("/api/admin/damages/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search damages", description = "Search damages with filters (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Page<DamageReportDto>> searchDamages(
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
            @Parameter(description = "Customer ID filter")
            @RequestParam(required = false) Long customerId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("GET /api/admin/damages/search");
        DamageSearchFilterDto filter = new DamageSearchFilterDto(
                startDate, endDate, severity, category, status, carId, customerId
        );
        Page<DamageReportDto> damages = damageHistoryService.searchDamages(filter, pageable);
        return ResponseEntity.ok(damages);
    }

    @GetMapping("/api/admin/damages/statistics")
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

        log.info("GET /api/admin/damages/statistics - startDate: {}, endDate: {}", startDate, endDate);
        DamageStatisticsDto statistics = damageHistoryService.getDamageStatistics(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }
}
