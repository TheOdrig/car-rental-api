package com.akif.car.web;

import com.akif.rental.api.RentalResponse;
import com.akif.rental.api.RentalService;
import com.akif.rental.domain.enums.RentalStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cars")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Car Management", description = "Car management operations for administrators")
public class AdminCarController {

    private final RentalService rentalService;

    @GetMapping("/{id}/rentals")
    @Operation(summary = "Get car rental history (Admin)", description = "Returns paginated rental history for a specific car")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rental history retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "404", description = "Car not found")
    })
    public ResponseEntity<Page<RentalResponse>> getCarRentals(
            @Parameter(description = "Car ID", required = true) @PathVariable Long id,
            @Parameter(description = "Filter by rental status") @RequestParam(required = false) RentalStatus status,
            @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetails adminUser) {

        log.info("GET /api/admin/cars/{}/rentals - Admin: {}, Status: {}", id, adminUser.getUsername(), status);

        Page<RentalResponse> rentals = rentalService.getCarRentals(id, status, pageable);

        return ResponseEntity.ok(rentals);
    }
}
