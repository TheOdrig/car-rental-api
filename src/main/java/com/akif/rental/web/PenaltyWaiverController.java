package com.akif.rental.web;

import com.akif.shared.security.AuthenticatedUser;
import com.akif.rental.internal.mapper.PenaltyWaiverMapper;
import com.akif.rental.internal.dto.penalty.PenaltyWaiverRequest;
import com.akif.rental.internal.dto.penalty.PenaltyWaiverResponse;
import com.akif.rental.domain.model.PenaltyWaiver;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/rentals")
@RequiredArgsConstructor
@Tag(name = "Penalty Waiver Management", description = "Admin operations for waiving penalties")
public class PenaltyWaiverController {

    private final PenaltyWaiverService penaltyWaiverService;
    private final PenaltyWaiverMapper penaltyWaiverMapper;

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
            @AuthenticationPrincipal AuthenticatedUser caller) {

        PenaltyWaiver waiver;
        if (Boolean.TRUE.equals(request.fullWaiver())) {
            waiver = penaltyWaiverService.waiveFullPenalty(id, request.reason(), caller.id());
        } else {
            waiver = penaltyWaiverService.waivePenalty(id, request.waiverAmount(), 
                    request.reason(), caller.id());
        }

        PenaltyWaiverResponse response = penaltyWaiverMapper.toResponse(waiver);
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


        List<PenaltyWaiver> history = penaltyWaiverService.getPenaltyHistory(id);
        List<PenaltyWaiverResponse> response = penaltyWaiverMapper.toResponses(history);

        return ResponseEntity.ok(response);
    }
}
