package com.akif.car.web;

import com.akif.car.api.CarResponse;
import com.akif.car.api.FilterOptionsResponse;
import com.akif.car.internal.dto.request.CarRequest;
import com.akif.car.internal.dto.request.CarSearchRequest;
import com.akif.currency.api.ConversionResult;
import com.akif.shared.enums.CurrencyType;
import com.akif.car.internal.service.CarQueryService;
import com.akif.car.internal.service.CarCommandService;
import com.akif.car.internal.service.CarStatisticsService;
import com.akif.currency.api.CurrencyConversionService;
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
import com.akif.shared.web.PageResponse;
import org.springframework.data.domain.Page;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cars")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Car Management", description = "Basic CRUD operations for cars")
public class CarController {

    private final CarQueryService carQueryService;
    private final CarCommandService carCommandService;
    private final CarStatisticsService carStatisticsService;
    private final CurrencyConversionService currencyConversionService;

    @GetMapping("/{id}")
    @Operation(summary = "Get car by ID", description = "Retrieve a car by its unique identifier with optional currency conversion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Car found successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarResponse.class))),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Invalid car ID or currency")
    })
    public ResponseEntity<CarResponse> getCarById(
            @Parameter(description = "Car ID", required = true) @PathVariable Long id,
            @Parameter(description = "Target currency for price conversion") @RequestParam(required = false) CurrencyType currency) {
        log.debug("GET /api/v1/cars/{} with currency={}", id, currency);
        CarResponse car = carQueryService.getCarById(id);
        applyPriceConversion(car, currency);
        log.info("Successfully retrieved car: ID={}", id);
        return ResponseEntity.ok(car);
    }

    @GetMapping("/licensePlate/{licensePlate}")
    @Operation(summary = "Get car by license plate", description = "Retrieve a car by its license plate number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Car found successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarResponse.class))),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Invalid license plate")
    })
    public ResponseEntity<CarResponse> getCarByLicensePlate(
            @Parameter(description = "License plate number", required = true) @PathVariable String licensePlate) {
        log.debug("GET /api/v1/cars/licensePlate/{}", licensePlate);
        CarResponse car = carQueryService.getCarByLicensePlate(licensePlate);
        log.info("Successfully retrieved car by license plate: {}", licensePlate);
        return ResponseEntity.ok(car);
    }

    @GetMapping("/search")
    @Operation(summary = "Search cars", description = "Search cars with various filters and pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cars found successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters")
    })
    public ResponseEntity<PageResponse<CarResponse>> searchCars(
            @Valid @ParameterObject CarSearchRequest searchRequest) {
        log.debug("GET /api/v1/cars/search - Searching cars with criteria: {}", searchRequest);
        PageResponse<CarResponse> response = carQueryService.searchCars(searchRequest);
        log.info("Successfully found {} cars", response.totalElements());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter-options")
    @Operation(summary = "Get filter options", 
               description = "Returns unique values for filter dropdowns (brands, transmission types, fuel types, body types)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filter options retrieved successfully",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = FilterOptionsResponse.class)))
    })
    public ResponseEntity<FilterOptionsResponse> getFilterOptions() {
        log.debug("GET /api/v1/cars/filter-options");
        FilterOptionsResponse options = carStatisticsService.getFilterOptions();
        log.info("Successfully retrieved filter options");
        return ResponseEntity.ok(options);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new car", description = "Create a new car with the provided information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Car created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid car data"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
            @ApiResponse(responseCode = "409", description = "Car already exists")
    })
    public ResponseEntity<CarResponse> createCar(
            @Parameter(description = "Car data", required = true) @Valid @RequestBody CarRequest carRequest) {
        log.debug("POST /api/v1/cars - Creating car with license plate: {}", carRequest.getLicensePlate());
        CarResponse car = carCommandService.createCar(carRequest);
        log.info("Successfully created car: ID={}, License Plate={}", car.getId(), car.getLicensePlate());
        return ResponseEntity.status(HttpStatus.CREATED).body(car);
    }

    @PutMapping(value = "/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update car", description = "Update an existing car with new information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Car updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Invalid car data")
    })
    public ResponseEntity<CarResponse> updateCar(
            @Parameter(description = "Car ID", required = true) @PathVariable Long id,
            @Parameter(description = "Updated car data", required = true) @Valid @RequestBody CarRequest carRequest) {
        log.debug("PUT /api/v1/cars/{} - Updating car", id);
        CarResponse car = carCommandService.updateCar(id, carRequest);
        log.info("Successfully updated car: ID={}", id);
        return ResponseEntity.ok(car);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete car", description = "Permanently delete a car")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Car deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Invalid car ID")
    })
    public ResponseEntity<Void> deleteCar(
            @Parameter(description = "Car ID", required = true) @PathVariable Long id) {
        log.debug("DELETE /api/v1/cars/{} - Deleting car", id);
        carCommandService.deleteCar(id);
        log.info("Successfully deleted car: ID={}", id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/soft")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete car", description = "Soft delete a car (mark as deleted)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Car soft deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Invalid car ID")
    })
    public ResponseEntity<Void> softDeleteCar(
            @Parameter(description = "Car ID", required = true) @PathVariable Long id) {
        carCommandService.softDeleteCar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Restore car", description = "Restore a soft deleted car")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Car restored successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Invalid car ID")
    })
    public ResponseEntity<CarResponse> restoreCar(
            @Parameter(description = "Car ID", required = true) @PathVariable Long id) {
        CarResponse car = carCommandService.restoreCar(id);
        return ResponseEntity.ok(car);
    }

    @GetMapping
    @Operation(summary = "Get all cars", description = "Retrieve all cars with pagination and optional currency conversion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cars retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters or currency")
    })
    public ResponseEntity<PageResponse<CarResponse>> getAllCars(
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) @ParameterObject Pageable pageable,
            @Parameter(description = "Target currency for price conversion") @RequestParam(required = false) CurrencyType currency) {
        log.debug("GET /api/v1/cars - Getting all cars with page: {}, size: {}, currency: {}", pageable.getPageNumber(), pageable.getPageSize(), currency);
        Page<CarResponse> cars = carQueryService.getAllCars(pageable);
        if (currency != null) {
            cars.forEach(car -> applyPriceConversion(car, currency));
        }
        log.info("Successfully retrieved {} cars", cars.getTotalElements());
        return ResponseEntity.ok(PageResponse.of(cars));
    }

    @GetMapping(value = "/active")
    @Operation(summary = "Get active cars", 
               description = "Retrieve all active (not deleted) cars with pagination, sorting, and optional currency conversion. " +
                       "Supported sort fields: price, brand, model, productionYear, rating, createTime. " +
                       "Example: ?sort=price,asc or ?sort=createTime,desc")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active cars retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters, sort field, or currency")
    })
    public ResponseEntity<PageResponse<CarResponse>> getAllActiveCars(
            @Parameter(description = "Pagination and sorting. Use ?sort=field,direction (e.g., ?sort=price,asc)") 
            @PageableDefault(size = 20, sort = "createTime", direction = Sort.Direction.DESC) @ParameterObject Pageable pageable,
            @Parameter(description = "Target currency for price conversion") @RequestParam(required = false) CurrencyType currency) {
        log.debug("GET /api/v1/cars/active - Getting active cars with page: {}, size: {}, sort: {}", 
                 pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        Page<CarResponse> cars = carQueryService.getAllActiveCars(pageable);
        if (currency != null) {
            cars.forEach(car -> applyPriceConversion(car, currency));
        }
        log.info("Successfully retrieved {} active cars", cars.getTotalElements());
        return ResponseEntity.ok(PageResponse.of(cars));
    }

    private void applyPriceConversion(CarResponse car, CurrencyType targetCurrency) {
        if (targetCurrency == null || car.getPrice() == null) {
            return;
        }

        CurrencyType originalCurrency = car.getCurrencyType() != null ? car.getCurrencyType() : CurrencyType.TRY;

        if (originalCurrency.equals(targetCurrency)) {
            car.setDisplayCurrency(targetCurrency);
            car.setConvertedPrice(car.getPrice());
            car.setExchangeRate(java.math.BigDecimal.ONE);
            car.setRateSource("LIVE");
            return;
        }

        ConversionResult result = currencyConversionService.convert(car.getPrice(), originalCurrency, targetCurrency);
        car.setConvertedPrice(result.convertedAmount());
        car.setDisplayCurrency(targetCurrency);
        car.setExchangeRate(result.exchangeRate());
        car.setRateSource(result.source().getDisplayName());
    }
}
