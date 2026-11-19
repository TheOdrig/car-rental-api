package com.akif.car.web;

import com.akif.shared.web.AllowedSort;
import com.akif.car.api.CarResponse;
import com.akif.car.api.FilterOptionsResponse;
import com.akif.car.internal.dto.request.CarRequest;
import com.akif.car.internal.dto.request.CarSearchRequest;
import com.akif.currency.api.DisplayPrice;
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
        CarResponse car = withDisplayPrice(carQueryService.getCarById(id), currency);
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
        CarResponse car = carQueryService.getCarByLicensePlate(licensePlate);
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
        PageResponse<CarResponse> response = carQueryService.searchCars(searchRequest);
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
        FilterOptionsResponse options = carStatisticsService.getFilterOptions();
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
        CarResponse car = carCommandService.createCar(carRequest);
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
        CarResponse car = carCommandService.updateCar(id, carRequest);
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
        carCommandService.deleteCar(id);
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

    @AllowedSort({"id", "brand", "model", "productionYear", "price", "rating", "createTime"})
    @GetMapping
    @Operation(summary = "Get all cars", description = "Retrieve all cars with pagination and optional currency conversion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cars retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters or currency")
    })
    public ResponseEntity<PageResponse<CarResponse>> getAllCars(
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) @ParameterObject Pageable pageable,
            @Parameter(description = "Target currency for price conversion") @RequestParam(required = false) CurrencyType currency) {
        Page<CarResponse> cars = carQueryService.getAllCars(pageable);
        if (currency != null) {
            cars = cars.map(car -> withDisplayPrice(car, currency));
        }
        return ResponseEntity.ok(PageResponse.of(cars));
    }

    @AllowedSort({"id", "brand", "model", "productionYear", "price", "rating", "createTime"})
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
        Page<CarResponse> cars = carQueryService.getAllActiveCars(pageable);
        if (currency != null) {
            cars = cars.map(car -> withDisplayPrice(car, currency));
        }
        return ResponseEntity.ok(PageResponse.of(cars));
    }

    private CarResponse withDisplayPrice(CarResponse car, CurrencyType targetCurrency) {
        DisplayPrice price = DisplayPrice.of(currencyConversionService, car.price(), car.currencyType(), targetCurrency);
        if (price == null) {
            return car;
        }
        return car.withConversion(price.currency(), price.amount(), price.exchangeRate(), price.rateSource());
    }
}
