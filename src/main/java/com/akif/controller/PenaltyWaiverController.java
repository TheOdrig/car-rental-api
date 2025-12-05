package com.akif.controller;

import com.akif.dto.request.PenaltyWaiverRequestDto;
import com.akif.dto.response.PenaltyWaiverResponseDto;
import com.akif.model.PenaltyWaiver;
import com.akif.model.User;
import com.akif.repository.UserRepository;
import com.akif.service.penalty.IPenaltyWaiverService;
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
@RequestMapping("/api/admin/rentals")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Penalty Waiver Management", description = "Admin operations for waiving penalties")
public class PenaltyWaiverController {

    private final IPenaltyWaiverService penaltyWaiverService;
    private final UserRepository userRepository;

    @PostMapping("/{id}/penalty/waive")
    @Operation(summary = "Waive penalty",
               description = "Waive full or partial penalty for a rental (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Penalty waived successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PenaltyWaiverResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid waiver request"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin only"),
            @ApiResponse(responseCode = "404", description = "Rental not found")
    })
    public ResponseEntity<PenaltyWaiverResponseDto> waivePenalty(
            @Parameter(description = "Rental ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Waiver request data", required = true)
            @Valid @RequestBody PenaltyWaiverRequestDto request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("POST /api/admin/rentals/{}/penalty/waive - Admin: {}", id, username);

        User admin = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        PenaltyWaiver waiver;
        if (Boolean.TRUE.equals(request.getFullWaiver())) {
            waiver = penaltyWaiverService.waiveFullPenalty(id, request.getReason(), admin.getId());
            log.info("Full penalty waived for rental: {}", id);
        } else {
            waiver = penaltyWaiverService.waivePenalty(id, request.getWaiverAmount(), 
                    request.getReason(), admin.getId());
            log.info("Partial penalty waived for rental: {}, amount: {}", id, request.getWaiverAmount());
        }

        PenaltyWaiverResponseDto response = mapToDto(waiver);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/penalty/history")
    @Operation(summary = "Get penalty history",
               description = "Retrieve all penalty waivers and adjustments for a rental (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Penalty history retrieved successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin only"),
            @ApiResponse(responseCode = "404", description = "Rental not found")
    })
    public ResponseEntity<List<PenaltyWaiverResponseDto>> getPenaltyHistory(
            @Parameter(description = "Rental ID", required = true)
            @PathVariable Long id) {

        log.debug("GET /api/admin/rentals/{}/penalty/history", id);

        List<PenaltyWaiver> history = penaltyWaiverService.getPenaltyHistory(id);
        List<PenaltyWaiverResponseDto> response = history.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        log.info("Retrieved {} penalty waiver records for rental: {}", response.size(), id);
        return ResponseEntity.ok(response);
    }

    private PenaltyWaiverResponseDto mapToDto(PenaltyWaiver waiver) {
        return PenaltyWaiverResponseDto.builder()
                .id(waiver.getId())
                .rentalId(waiver.getRental().getId())
                .originalPenalty(waiver.getOriginalPenalty())
                .waivedAmount(waiver.getWaivedAmount())
                .remainingPenalty(waiver.getRemainingPenalty())
                .reason(waiver.getReason())
                .adminId(waiver.getAdminId())
                .waivedAt(waiver.getWaivedAt())
                .refundInitiated(waiver.getRefundInitiated())
                .refundTransactionId(waiver.getRefundTransactionId())
                .build();
    }
}
