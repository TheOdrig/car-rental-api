package com.akif.controller;

import com.akif.dto.damage.request.DamageAssessmentRequestDto;
import com.akif.dto.damage.response.DamageAssessmentResponseDto;
import com.akif.service.damage.IDamageAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Damage Assessment", description = "Operations for damage assessment")
public class DamageAssessmentController {

    private final IDamageAssessmentService damageAssessmentService;

    @PostMapping("/api/admin/damages/{id}/assess")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assess damage", description = "Assess damage and calculate customer liability (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Damage assessed"),
            @ApiResponse(responseCode = "400", description = "Invalid status or data"),
            @ApiResponse(responseCode = "404", description = "Damage report not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<DamageAssessmentResponseDto> assessDamage(
            @Parameter(description = "Damage ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody DamageAssessmentRequestDto request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("POST /api/admin/damages/{}/assess - user: {}", id, username);
        DamageAssessmentResponseDto response = damageAssessmentService.assessDamage(id, request, username);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/api/admin/damages/{id}/assess")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update assessment", description = "Update damage assessment (admin only, before charge)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assessment updated"),
            @ApiResponse(responseCode = "400", description = "Cannot update after charge"),
            @ApiResponse(responseCode = "404", description = "Damage report not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<DamageAssessmentResponseDto> updateAssessment(
            @Parameter(description = "Damage ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody DamageAssessmentRequestDto request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("PUT /api/admin/damages/{}/assess - user: {}", id, username);
        DamageAssessmentResponseDto response = damageAssessmentService.updateAssessment(id, request, username);
        return ResponseEntity.ok(response);
    }
}
