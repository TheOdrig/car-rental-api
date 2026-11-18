package com.akif.rental.web;

import com.akif.rental.internal.dto.penalty.PenaltyWaiverRequest;
import com.akif.rental.internal.dto.penalty.PenaltyWaiverResponse;
import com.akif.rental.domain.model.PenaltyWaiver;
import com.akif.auth.api.UserLookup;
import com.akif.auth.api.UserDto;
import com.akif.rental.internal.service.penalty.PenaltyWaiverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/rentals")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Penalty Waiver Management", description = "Admin operations for waiving penalties")
public class PenaltyWaiverController {

    private final PenaltyWaiverService penaltyWaiverService;
    private final UserLookup userLookup;

    @PostMapping("/{id}/penalty/waive")
    @Operation(summary = "Waive penalty",
               description = "Waive full or partial penalty for a rental (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Penalty waived successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PenaltyWaiverResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid waiver request"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin only"),
            @ApiResponse(responseCode = "404", description = "Rental not found")
    })
    public ResponseEntity<PenaltyWaiverResponse> waivePenalty(
            @Parameter(description = "Rental ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Waiver request data", required = true)
            @Valid @RequestBody PenaltyWaiverRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("POST /api/v1/admin/rentals/{}/penalty/waive - Admin: {}", id, username);

        UserDto admin = userLookup.getUserByUsername(username);

        PenaltyWaiver waiver;
        if (Boolean.TRUE.equals(request.fullWaiver())) {
            waiver = penaltyWaiverService.waiveFullPenalty(id, request.reason(), admin.id());
            log.info("Full penalty waived for rental: {}", id);
        } else {
            waiver = penaltyWaiverService.waivePenalty(id, request.waiverAmount(), 
                    request.reason(), admin.id());
            log.info("Partial penalty waived for rental: {}, amount: {}", id, request.waiverAmount());
        }

        PenaltyWaiverResponse response = mapToDto(waiver);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/penalty/history")
    @Operation(summary = "Get penalty history",
               description = "Retrieve all penalty waivers and adjustments for a rental (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Penalty history retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin only"),
            @ApiResponse(responseCode = "404", description = "Rental not found")
    })
    public ResponseEntity<List<PenaltyWaiverResponse>> getPenaltyHistory(
            @Parameter(description = "Rental ID", required = true)
            @PathVariable Long id) {

        log.debug("GET /api/v1/admin/rentals/{}/penalty/history", id);

        List<PenaltyWaiver> history = penaltyWaiverService.getPenaltyHistory(id);
        List<PenaltyWaiverResponse> response = history.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        log.info("Retrieved {} penalty waiver records for rental: {}", response.size(), id);
        return ResponseEntity.ok(response);
    }

    private PenaltyWaiverResponse mapToDto(PenaltyWaiver waiver) {
        return new PenaltyWaiverResponse(
                waiver.getId(),
                waiver.getRental().getId(),
                waiver.getOriginalPenalty(),
                waiver.getWaivedAmount(),
                waiver.getRemainingPenalty(),
                waiver.getReason(),
                waiver.getAdminId(),
                waiver.getWaivedAt(),
                waiver.getRefundInitiated(),
                waiver.getRefundTransactionId()
        );
    }
}
