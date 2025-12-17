package com.akif.dashboard.web;

import com.akif.dashboard.api.QuickActionService;
import com.akif.dashboard.api.dto.QuickActionResultDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/quick-actions")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Quick Actions", description = "Quick rental operations for administrators")
public class QuickActionController {

    private final QuickActionService quickActionService;

    @PostMapping("/rentals/{id}/approve")
    @Operation(summary = "Approve rental", description = "Approves a pending rental request and returns updated dashboard summary")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rental approved"),
            @ApiResponse(responseCode = "400", description = "Invalid rental state"),
            @ApiResponse(responseCode = "404", description = "Rental not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<QuickActionResultDto> approveRental(
            @Parameter(description = "Rental ID", required = true)
            @PathVariable Long id) {
        
        log.info("POST /api/admin/quick-actions/rentals/{}/approve", id);
        QuickActionResultDto result = quickActionService.approveRental(id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/rentals/{id}/pickup")
    @Operation(summary = "Process pickup", description = "Processes car pickup for a confirmed rental and returns updated dashboard summary")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pickup processed"),
            @ApiResponse(responseCode = "400", description = "Invalid rental state"),
            @ApiResponse(responseCode = "404", description = "Rental not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<QuickActionResultDto> processPickup(
            @Parameter(description = "Rental ID", required = true)
            @PathVariable Long id) {
        
        log.info("POST /api/admin/quick-actions/rentals/{}/pickup", id);
        QuickActionResultDto result = quickActionService.processPickup(id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/rentals/{id}/return")
    @Operation(summary = "Process return", description = "Processes car return for an in-use rental and returns updated dashboard summary")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return processed"),
            @ApiResponse(responseCode = "400", description = "Invalid rental state"),
            @ApiResponse(responseCode = "404", description = "Rental not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<QuickActionResultDto> processReturn(
            @Parameter(description = "Rental ID", required = true)
            @PathVariable Long id) {
        
        log.info("POST /api/admin/quick-actions/rentals/{}/return", id);
        QuickActionResultDto result = quickActionService.processReturn(id);
        return ResponseEntity.ok(result);
    }
}
