package com.akif.controller;

import com.akif.dto.damage.request.DamageDisputeRequestDto;
import com.akif.dto.damage.request.DamageDisputeResolutionDto;
import com.akif.dto.damage.response.DamageDisputeResponseDto;
import com.akif.service.damage.IDamageDisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Damage Dispute", description = "Operations for damage dispute management")
public class DamageDisputeController {

    private final IDamageDisputeService damageDisputeService;

    @PostMapping("/api/damages/{id}/dispute")
    @Operation(summary = "Create dispute", description = "Customer creates a dispute for damage report")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Dispute created"),
            @ApiResponse(responseCode = "400", description = "Invalid status"),
            @ApiResponse(responseCode = "403", description = "Not owner of rental"),
            @ApiResponse(responseCode = "404", description = "Damage report not found")
    })
    public ResponseEntity<DamageDisputeResponseDto> createDispute(
            @Parameter(description = "Damage ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody DamageDisputeRequestDto request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("POST /api/damages/{}/dispute - User: {}", id, username);
        DamageDisputeResponseDto response = damageDisputeService.createDispute(id, request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/admin/damages/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resolve dispute", description = "Admin resolves a dispute (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dispute resolved"),
            @ApiResponse(responseCode = "400", description = "Invalid status"),
            @ApiResponse(responseCode = "404", description = "Damage report not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<DamageDisputeResponseDto> resolveDispute(
            @Parameter(description = "Damage ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody DamageDisputeResolutionDto resolution,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("POST /api/admin/damages/{}/resolve - user: {}", id, username);
        DamageDisputeResponseDto response = damageDisputeService.resolveDispute(id, resolution, username);
        return ResponseEntity.ok(response);
    }
}
