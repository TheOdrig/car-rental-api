package com.akif.car.web;

import com.akif.car.api.CarResponse;
import com.akif.car.api.CarStatusType;
import com.akif.car.api.CarOccupancy;
import com.akif.car.internal.service.AdminFleetService;
import com.akif.shared.web.AllowedSort;
import com.akif.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/cars")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Cars", description = "Fleet management reads for administrators")
public class AdminCarController {

    private final AdminFleetService adminFleetService;

    @AllowedSort({"id", "licensePlate", "brand", "model", "productionYear", "price", "rating", "createTime"})
    @GetMapping
    @Operation(summary = "Search the fleet",
            description = "Server-side search, status, brand and occupancy filters, paging and sorting over all non-deleted cars, including the VIN and the current occupancy")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fleet page"),
            @ApiResponse(responseCode = "400", description = "Invalid paging, sort or filter parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    public ResponseEntity<PageResponse<CarResponse>> searchFleet(
            @Parameter(description = "Free text matched against license plate, brand and model")
            @RequestParam(required = false) String search,
            @Parameter(description = "Exact brand, case-insensitive")
            @RequestParam(required = false) String brand,
            @Parameter(description = "Car status filter")
            @RequestParam(required = false) CarStatusType status,
            @Parameter(description = "Current occupancy: FREE, RESERVED (confirmed rental covering today) or IN_USE (picked up, not returned)")
            @RequestParam(required = false) CarOccupancy occupancy,
            @PageableDefault(size = 20) @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(adminFleetService.search(search, brand, status, occupancy, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a car including the VIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Car found"),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    public ResponseEntity<CarResponse> getCar(@PathVariable Long id) {
        return ResponseEntity.ok(adminFleetService.get(id));
    }
}
