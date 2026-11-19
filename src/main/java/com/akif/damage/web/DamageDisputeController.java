package com.akif.damage.web;

import com.akif.damage.internal.dto.damage.request.DamageDisputeRequest;
import com.akif.damage.internal.dto.damage.request.DamageDisputeResolutionDto;
import com.akif.damage.internal.dto.damage.response.DamageDisputeResponse;
import com.akif.damage.internal.service.damage.DamageDisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Damage Dispute", description = "Operations for damage dispute management")
public class DamageDisputeController {

    private final DamageDisputeService damageDisputeService;

    @PostMapping("/api/v1/damages/{id}/dispute")
    @Operation(summary = "Create dispute", description = "Customer creates a dispute for damage report")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Dispute created"),
            @ApiResponse(responseCode = "400", description = "Invalid status"),
            @ApiResponse(responseCode = "403", description = "Not owner of rental"),
            @ApiResponse(responseCode = "404", description = "Damage report not found")
    })
    public ResponseEntity<DamageDisputeResponse> createDispute(
            @Parameter(description = "Damage ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody DamageDisputeRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        DamageDisputeResponse response = damageDisputeService.createDispute(id, request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/v1/admin/damages/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resolve dispute", description = "Admin resolves a dispute (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dispute resolved"),
            @ApiResponse(responseCode = "400", description = "Invalid status"),
            @ApiResponse(responseCode = "404", description = "Damage report not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<DamageDisputeResponse> resolveDispute(
            @Parameter(description = "Damage ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody DamageDisputeResolutionDto resolution,
            Authentication authentication) {

        String username = authentication.getName();
        DamageDisputeResponse response = damageDisputeService.resolveDispute(id, resolution, username);
        return ResponseEntity.ok(response);
    }
}
