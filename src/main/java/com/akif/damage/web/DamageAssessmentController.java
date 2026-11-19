package com.akif.damage.web;

import com.akif.damage.internal.dto.damage.request.DamageAssessmentRequest;
import com.akif.damage.internal.dto.damage.response.DamageAssessmentResponse;
import com.akif.damage.internal.service.damage.DamageAssessmentService;
import com.akif.damage.internal.service.damage.DamageChargeService;
import com.akif.damage.internal.dto.damage.response.DamageChargeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Damage Assessment", description = "Operations for damage assessment")
public class DamageAssessmentController {

    private final DamageAssessmentService damageAssessmentService;
    private final DamageChargeService damageChargeService;

    @PostMapping("/api/v1/admin/damages/{id}/assess")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assess damage", description = "Assess damage and calculate customer liability (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Damage assessed"),
            @ApiResponse(responseCode = "400", description = "Invalid status or data"),
            @ApiResponse(responseCode = "404", description = "Damage report not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<DamageAssessmentResponse> assessDamage(
            @Parameter(description = "Damage ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody DamageAssessmentRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        DamageAssessmentResponse response = damageAssessmentService.assessDamage(id, request, username);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/v1/admin/damages/{id}/charge")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Charge damage", description = "Charge the assessed customer liability to the card saved with the rental (admin only). A charged report can then be disputed by the customer.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Charge attempted; see the charged flag"),
            @ApiResponse(responseCode = "400", description = "Report is not in ASSESSED status or has no liability"),
            @ApiResponse(responseCode = "404", description = "Damage report not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<DamageChargeResponse> chargeDamage(
            @Parameter(description = "Damage ID", required = true)
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(damageChargeService.charge(id));
    }

    @PutMapping("/api/v1/admin/damages/{id}/assess")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update assessment", description = "Update damage assessment (admin only, before charge)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assessment updated"),
            @ApiResponse(responseCode = "400", description = "Cannot update after charge"),
            @ApiResponse(responseCode = "404", description = "Damage report not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<DamageAssessmentResponse> updateAssessment(
            @Parameter(description = "Damage ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody DamageAssessmentRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        DamageAssessmentResponse response = damageAssessmentService.updateAssessment(id, request, username);
        return ResponseEntity.ok(response);
    }
}
