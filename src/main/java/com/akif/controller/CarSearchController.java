package com.akif.controller;

import com.akif.dto.request.CarSearchRequestDto;
import com.akif.dto.response.CarListResponseDto;
import com.akif.dto.response.CarResponseDto;
import com.akif.dto.response.CarSummaryResponseDto;
import com.akif.shared.enums.CarStatusType;
import com.akif.service.ICarService;
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
import java.util.List;

@RestController
@RequestMapping("/api/cars/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Car Search", description = "Search and filtering operations for cars")
public class CarSearchController {

    private final ICarService carService;

    @PostMapping
    @Operation(summary = "Search cars", description = "Search cars with various criteria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarListResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria")
    })
    public ResponseEntity<CarListResponseDto> searchCars(
            @Parameter(description = "Search criteria", required = true) @Valid @RequestBody CarSearchRequestDto searchRequest) {
        log.debug("POST /api/cars/search - Searching cars with criteria: {}", searchRequest);
        CarListResponseDto result = carService.searchCars(searchRequest);
        log.info("Search completed: found {} cars", result.getTotalElements());
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/status/{status}")
    @Operation(summary = "Get cars by status", description = "Retrieve cars by their status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cars retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status or pagination parameters")
    })
    public ResponseEntity<Page<CarResponseDto>> getCarsByStatus(
            @Parameter(description = "Car status", required = true) @PathVariable String status,
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) Pageable pageable) {
        Page<CarResponseDto> cars = carService.getCarsByStatus(status, pageable);
        return ResponseEntity.ok(cars);
    }

    @GetMapping(value = "/brand/{brand}")
    @Operation(summary = "Get cars by brand", description = "Retrieve cars by their brand")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cars retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid brand or pagination parameters")
    })
    public ResponseEntity<Page<CarResponseDto>> getCarsByBrand(
            @Parameter(description = "Car brand", required = true) @PathVariable String brand,
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) Pageable pageable) {
        Page<CarResponseDto> cars = carService.getCarsByBrand(brand, pageable);
        return ResponseEntity.ok(cars);
    }

    @GetMapping(value = "/price-range")
    @Operation(summary = "Get cars by price range", description = "Retrieve cars within a price range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cars retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid price range or pagination parameters")
    })
    public ResponseEntity<Page<CarResponseDto>> getCarsByPriceRange(
            @Parameter(description = "Minimum price", required = true) @RequestParam BigDecimal minPrice,
            @Parameter(description = "Maximum price", required = true) @RequestParam BigDecimal maxPrice,
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) Pageable pageable) {
        Page<CarResponseDto> cars = carService.getCarsByPriceRange(minPrice, maxPrice, pageable);
        return ResponseEntity.ok(cars);
    }

    @GetMapping(value = "/featured")
    @Operation(summary = "Get featured cars", description = "Retrieve featured cars")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Featured cars retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    public ResponseEntity<Page<CarResponseDto>> getFeaturedCars(
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) Pageable pageable) {
        Page<CarResponseDto> cars = carService.getFeaturedCars(pageable);
        return ResponseEntity.ok(cars);
    }

    @GetMapping(value = "/new")
    @Operation(summary = "Get new cars", description = "Retrieve new cars (less than 1 year old)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "New cars retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    public ResponseEntity<Page<CarResponseDto>> getNewCars(
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) Pageable pageable) {
        Page<CarResponseDto> cars = carService.getNewCars(pageable);
        return ResponseEntity.ok(cars);
    }

    @GetMapping(value = "/test-drive")
    @Operation(summary = "Get cars available for test drive", description = "Retrieve cars available for test drive")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cars available for test drive retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    public ResponseEntity<Page<CarResponseDto>> getCarsAvailableForTestDrive(
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) Pageable pageable) {
        Page<CarResponseDto> cars = carService.getCarsAvailableForTestDrive(pageable);
        return ResponseEntity.ok(cars);
    }

    @GetMapping(value = "/criteria")
    @Operation(summary = "Search cars by criteria", description = "Search cars with multiple criteria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria")
    })
    public ResponseEntity<Page<CarResponseDto>> searchCarsByCriteria(
            @Parameter(description = "Search term") @RequestParam(required = false) String searchTerm,
            @Parameter(description = "Brand") @RequestParam(required = false) String brand,
            @Parameter(description = "Model") @RequestParam(required = false) String model,
            @Parameter(description = "Minimum price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Status") @RequestParam(required = false) CarStatusType status,
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) Pageable pageable) {
        Page<CarResponseDto> cars = carService.searchCarsByCriteria(searchTerm, brand, model, minPrice, maxPrice, status, pageable);
        return ResponseEntity.ok(cars);
    }

    @GetMapping(value = "/most-viewed")
    @Operation(summary = "Get most viewed cars", description = "Retrieve most viewed cars")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Most viewed cars retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "400", description = "Invalid limit parameter")
    })
    public ResponseEntity<List<CarSummaryResponseDto>> getMostViewedCars(
            @Parameter(description = "Number of cars to return", required = true) @RequestParam(defaultValue = "10") int limit) {
        List<CarSummaryResponseDto> cars = carService.getMostViewedCars(limit);
        return ResponseEntity.ok(cars);
    }

    @GetMapping(value = "/most-liked")
    @Operation(summary = "Get most liked cars", description = "Retrieve most liked cars")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Most liked cars retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "400", description = "Invalid limit parameter")
    })
    public ResponseEntity<List<CarSummaryResponseDto>> getMostLikedCars(
            @Parameter(description = "Number of cars to return", required = true) @RequestParam(defaultValue = "10") int limit) {
        List<CarSummaryResponseDto> cars = carService.getMostLikedCars(limit);
        return ResponseEntity.ok(cars);
    }
}
