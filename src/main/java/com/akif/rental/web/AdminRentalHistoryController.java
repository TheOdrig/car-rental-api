package com.akif.rental.web;

import com.akif.shared.web.AllowedSort;
import com.akif.auth.api.AdminUserService;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.api.RentalService;
import com.akif.rental.api.RentalStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.akif.shared.web.PageResponse;
import org.springframework.data.domain.Page;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.akif.shared.security.AuthenticatedUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Rental History", description = "Rental history per car and per user for administrators")
public class AdminRentalHistoryController {

    private final RentalService rentalService;
    private final AdminUserService adminUserService;

    @AllowedSort({"createTime", "startDate", "endDate", "totalPrice", "status"})
    @GetMapping("/cars/{id}/rentals")
    @Operation(summary = "Get car rental history (Admin)", description = "Returns paginated rental history for a specific car")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rental history retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "404", description = "Car not found")
    })
    public ResponseEntity<PageResponse<RentalResponse>> getCarRentals(
            @Parameter(description = "Car ID", required = true) @PathVariable Long id,
            @Parameter(description = "Filter by rental status") @RequestParam(required = false) RentalStatus status,
            @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.DESC) @ParameterObject Pageable pageable) {


        Page<RentalResponse> rentals = rentalService.getCarRentals(id, status, pageable);

        return ResponseEntity.ok(PageResponse.of(rentals));
    }

    @AllowedSort({"createTime", "startDate", "endDate", "totalPrice", "status"})
    @GetMapping("/users/{id}/rentals")
    @Operation(summary = "Get user rental history (Admin)", description = "Returns paginated rental history for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rental history retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<PageResponse<RentalResponse>> getUserRentals(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            @Parameter(description = "Filter by rental status") @RequestParam(required = false) RentalStatus status,
            @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.DESC) @ParameterObject Pageable pageable) {


        adminUserService.getUserDetailForAdmin(id);

        Page<RentalResponse> rentals = rentalService.getUserRentals(id, status, pageable);

        return ResponseEntity.ok(PageResponse.of(rentals));
    }
}
