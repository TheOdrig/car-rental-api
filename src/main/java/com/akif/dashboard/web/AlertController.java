package com.akif.dashboard.web;

import com.akif.dashboard.api.AlertService;
import com.akif.dashboard.api.dto.AlertDto;
import com.akif.dashboard.domain.enums.AlertType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/alerts")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Alerts", description = "Alert management for administrators")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "Get active alerts", description = "Returns all unacknowledged alerts sorted by severity")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alerts retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<List<AlertDto>> getActiveAlerts(
            @Parameter(description = "Filter by alert type")
            @RequestParam(required = false) AlertType type) {
        
        log.info("GET /api/admin/alerts - type: {}", type);
        
        List<AlertDto> alerts;
        if (type != null) {
            alerts = alertService.getAlertsByType(type);
        } else {
            alerts = alertService.getActiveAlerts();
        }
        
        return ResponseEntity.ok(alerts);
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge alert", description = "Marks an alert as acknowledged by an admin")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alert acknowledged"),
            @ApiResponse(responseCode = "404", description = "Alert not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<AlertDto> acknowledgeAlert(
            @Parameter(description = "Alert ID", required = true)
            @PathVariable Long id,
            Authentication authentication) {
        
        String adminUsername = authentication.getName();
        log.info("POST /api/admin/alerts/{}/acknowledge - admin: {}", id, adminUsername);
        
        AlertDto acknowledgedAlert = alertService.acknowledgeAlert(id, adminUsername);
        return ResponseEntity.ok(acknowledgedAlert);
    }
}
