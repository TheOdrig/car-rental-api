package com.akif.car.web;

import com.akif.car.api.CarResponse;
import com.akif.car.internal.dto.request.CarSearchRequest;
import com.akif.car.internal.dto.response.CarListResponse;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.car.api.CarService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/cars/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Car Search", description = "Search and filtering operations for cars")
public class CarSearchController {

    private final CarService carService;

    @PostMapping
    @Operation(summary = "Search cars", description = "Search cars with various criteria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarListResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria")
    })
    public ResponseEntity<CarListResponse> searchCars(
            @Parameter(description = "Search criteria", required = true) @Valid @RequestBody CarSearchRequest searchRequest) {
        log.debug("POST /api/cars/search - Searching cars with criteria: {}", searchRequest);
        CarListResponse result = carService.searchCars(searchRequest);
        log.info("Search completed: found {} cars", result.totalElements());
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/status/{status}")
    @Operation(summary = "Get cars by status", description = "Retrieve cars by their status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cars retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status or pagination parameters")
    })
    public ResponseEntity<Page<CarResponse>> getCarsByStatus(
            @Parameter(description = "Car status", required = true) @PathVariable String status,
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) Pageable pageable) {
        Page<CarResponse> cars = carService.getCarsByStatus(status, pageable);
        return ResponseEntity.ok(cars);
    }

    @GetMapping(value = "/brand/{brand}")
    @Operation(summary = "Get cars by brand", description = "Retrieve cars by their brand")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cars retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid brand or pagination parameters")
    })
    public ResponseEntity<Page<CarResponse>> getCarsByBrand(
            @Parameter(description = "Car brand", required = true) @PathVariable String brand,
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) Pageable pageable) {
        Page<CarResponse> cars = carService.getCarsByBrand(brand, pageable);
        return ResponseEntity.ok(cars);
    }

    @GetMapping(value = "/price-range")
    @Operation(summary = "Get cars by price range", description = "Retrieve cars within a price range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cars retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid price range or pagination parameters")
    })
    public ResponseEntity<Page<CarResponse>> getCarsByPriceRange(
            @Parameter(description = "Minimum price", required = true) @RequestParam BigDecimal minPrice,
            @Parameter(description = "Maximum price", required = true) @RequestParam BigDecimal maxPrice,
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) Pageable pageable) {
        Page<CarResponse> cars = carService.getCarsByPriceRange(minPrice, maxPrice, pageable);
        return ResponseEntity.ok(cars);
    }

    @GetMapping(value = "/new")
    @Operation(summary = "Get new cars", description = "Retrieve new cars (less than 1 year old)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "New cars retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    public ResponseEntity<Page<CarResponse>> getNewCars(
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) Pageable pageable) {
        Page<CarResponse> cars = carService.getNewCars(pageable);
        return ResponseEntity.ok(cars);
    }

    @GetMapping(value = "/criteria")
    @Operation(summary = "Search cars by criteria", description = "Search cars with multiple criteria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria")
    })
    public ResponseEntity<Page<CarResponse>> searchCarsByCriteria(
            @Parameter(description = "Search term") @RequestParam(required = false) String searchTerm,
            @Parameter(description = "Brand") @RequestParam(required = false) String brand,
            @Parameter(description = "Model") @RequestParam(required = false) String model,
            @Parameter(description = "Minimum price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Status") @RequestParam(required = false) CarStatusType status,
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) Pageable pageable) {
        Page<CarResponse> cars = carService.searchCarsByCriteria(searchTerm, brand, model, minPrice, maxPrice, status, pageable);
        return ResponseEntity.ok(cars);
    }

}
