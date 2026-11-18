package com.akif.car.web;

import com.akif.car.api.CarResponse;
import com.akif.car.api.CarStatusType;
import com.akif.car.internal.service.CarQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.akif.shared.web.PageResponse;
import org.springframework.data.domain.Page;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/cars/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Car Search", description = "Search and filtering operations for cars")
public class CarSearchController {

    private final CarQueryService carQueryService;

    @GetMapping(value = "/status/{status}")
    @Operation(summary = "Get cars by status", description = "Retrieve cars by their status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cars retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status or pagination parameters")
    })
    public ResponseEntity<PageResponse<CarResponse>> getCarsByStatus(
            @Parameter(description = "Car status", required = true) @PathVariable String status,
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) @ParameterObject Pageable pageable) {
        Page<CarResponse> cars = carQueryService.getCarsByStatus(status, pageable);
        return ResponseEntity.ok(PageResponse.of(cars));
    }

    @GetMapping(value = "/brand/{brand}")
    @Operation(summary = "Get cars by brand", description = "Retrieve cars by their brand")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cars retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid brand or pagination parameters")
    })
    public ResponseEntity<PageResponse<CarResponse>> getCarsByBrand(
            @Parameter(description = "Car brand", required = true) @PathVariable String brand,
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) @ParameterObject Pageable pageable) {
        Page<CarResponse> cars = carQueryService.getCarsByBrand(brand, pageable);
        return ResponseEntity.ok(PageResponse.of(cars));
    }

    @GetMapping(value = "/price-range")
    @Operation(summary = "Get cars by price range", description = "Retrieve cars within a price range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cars retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid price range or pagination parameters")
    })
    public ResponseEntity<PageResponse<CarResponse>> getCarsByPriceRange(
            @Parameter(description = "Minimum price", required = true) @RequestParam BigDecimal minPrice,
            @Parameter(description = "Maximum price", required = true) @RequestParam BigDecimal maxPrice,
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) @ParameterObject Pageable pageable) {
        Page<CarResponse> cars = carQueryService.getCarsByPriceRange(minPrice, maxPrice, pageable);
        return ResponseEntity.ok(PageResponse.of(cars));
    }

    @GetMapping(value = "/new")
    @Operation(summary = "Get new cars", description = "Retrieve new cars (less than 1 year old)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "New cars retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    public ResponseEntity<PageResponse<CarResponse>> getNewCars(
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) @ParameterObject Pageable pageable) {
        Page<CarResponse> cars = carQueryService.getNewCars(pageable);
        return ResponseEntity.ok(PageResponse.of(cars));
    }

    @GetMapping(value = "/criteria")
    @Operation(summary = "Search cars by criteria", description = "Search cars with multiple criteria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria")
    })
    public ResponseEntity<PageResponse<CarResponse>> searchCarsByCriteria(
            @Parameter(description = "Search term") @RequestParam(required = false) String searchTerm,
            @Parameter(description = "Brand") @RequestParam(required = false) String brand,
            @Parameter(description = "Model") @RequestParam(required = false) String model,
            @Parameter(description = "Minimum price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Status") @RequestParam(required = false) CarStatusType status,
            @Parameter(description = "Page number, zero based") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<CarResponse> cars = carQueryService.searchCarsByCriteria(searchTerm, brand, model, minPrice, maxPrice, status, page, size);
        return ResponseEntity.ok(PageResponse.of(cars));
    }

}
