package com.akif.rental.web;

import com.akif.rental.api.EarlyReturnPreview;
import com.akif.rental.api.RentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/rentals")
@RequiredArgsConstructor
@Tag(name = "Rental Management", description = "Operations for managing car rentals")
public class EarlyReturnPreviewController {

    private final RentalService rentalService;

    @GetMapping("/{id}/early-return/preview")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Preview early return refund", description = "Returns the amount that an early return of this IN_USE rental would refund if it happened now. It uses the same calculation as the early return itself and changes nothing.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Refund preview", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EarlyReturnPreview.class))),
            @ApiResponse(responseCode = "400", description = "Rental is not in use"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
            @ApiResponse(responseCode = "404", description = "Rental or its payment not found")
    })
    public ResponseEntity<EarlyReturnPreview> previewEarlyReturn(
            @Parameter(description = "Rental ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(rentalService.previewEarlyReturn(id));
    }
}
