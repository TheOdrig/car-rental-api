package com.akif.controller;

import com.akif.dto.currency.ConversionResult;
import com.akif.dto.request.CarRequestDto;
import com.akif.dto.response.CarResponseDto;
import com.akif.enums.CurrencyType;
import com.akif.service.ICarService;
import com.akif.service.currency.ICurrencyConversionService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Car Management", description = "Basic CRUD operations for cars")
public class CarController {

    private final ICarService carService;
    private final ICurrencyConversionService currencyConversionService;


    @GetMapping("/{id}")
    @Operation(summary = "Get car by ID", description = "Retrieve a car by its unique identifier with optional currency conversion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Car found successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Invalid car ID or currency")
    })
    public ResponseEntity<CarResponseDto> getCarById(
            @Parameter(description = "Car ID", required = true) @PathVariable Long id,
            @Parameter(description = "Target currency for price conversion") @RequestParam(required = false) CurrencyType currency) {
        log.debug("GET /api/cars/{} with currency={}", id, currency);
        CarResponseDto car = carService.getCarById(id);
        applyPriceConversion(car, currency);
        log.info("Successfully retrieved car: ID={}", id);
        return ResponseEntity.ok(car);
    }

    @GetMapping("/licensePlate/{licensePlate}")
    @Operation(summary = "Get car by license plate", description = "Retrieve a car by its license plate number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Car found successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Invalid license plate")
    })
    public ResponseEntity<CarResponseDto> getCarByLicensePlate(
            @Parameter(description = "License plate number", required = true) @PathVariable String licensePlate) {
        log.debug("GET /api/cars/licensePlate/{}", licensePlate);
        CarResponseDto car = carService.getCarByLicensePlate(licensePlate);
        log.info("Successfully retrieved car by license plate: {}", licensePlate);
        return ResponseEntity.ok(car);
    }

    @PostMapping
    @Operation(summary = "Create new car", description = "Create a new car with the provided information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Car created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid car data"),
            @ApiResponse(responseCode = "409", description = "Car already exists")
    })
    public ResponseEntity<CarResponseDto> createCar(
            @Parameter(description = "Car data", required = true) @Valid @RequestBody CarRequestDto carRequest) {
        log.debug("POST /api/cars - Creating car with license plate: {}", carRequest.getLicensePlate());
        CarResponseDto car = carService.createCar(carRequest);
        log.info("Successfully created car: ID={}, License Plate={}", car.getId(), car.getLicensePlate());
        return ResponseEntity.status(HttpStatus.CREATED).body(car);
    }

    @PutMapping(value = "/{id}")
    @Operation(summary = "Update car", description = "Update an existing car with new information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Car updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Invalid car data")
    })
    public ResponseEntity<CarResponseDto> updateCar(
            @Parameter(description = "Car ID", required = true) @PathVariable Long id,
            @Parameter(description = "Updated car data", required = true) @Valid @RequestBody CarRequestDto carRequest) {
        log.debug("PUT /api/cars/{} - Updating car", id);
        CarResponseDto car = carService.updateCar(id, carRequest);
        log.info("Successfully updated car: ID={}", id);
        return ResponseEntity.ok(car);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete car", description = "Permanently delete a car")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Car deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Invalid car ID")
    })
    public ResponseEntity<Void> deleteCar(
            @Parameter(description = "Car ID", required = true) @PathVariable Long id) {
        log.debug("DELETE /api/cars/{} - Deleting car", id);
        carService.deleteCar(id);
        log.info("Successfully deleted car: ID={}", id);
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/{id}/soft")
    @Operation(summary = "Soft delete car", description = "Soft delete a car (mark as deleted)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Car soft deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Invalid car ID")
    })
    public ResponseEntity<Void> softDeleteCar(
            @Parameter(description = "Car ID", required = true) @PathVariable Long id) {
        carService.softDeleteCar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore car", description = "Restore a soft deleted car")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Car restored successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Invalid car ID")
    })
    public ResponseEntity<CarResponseDto> restoreCar(
            @Parameter(description = "Car ID", required = true) @PathVariable Long id) {
        carService.restoreCar(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping
    @Operation(summary = "Get all cars", description = "Retrieve all cars with pagination and optional currency conversion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cars retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters or currency")
    })
    public ResponseEntity<Page<CarResponseDto>> getAllCars(
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "Target currency for price conversion") @RequestParam(required = false) CurrencyType currency) {
        log.debug("GET /api/cars - Getting all cars with page: {}, size: {}, currency: {}", pageable.getPageNumber(), pageable.getPageSize(), currency);
        Page<CarResponseDto> cars = carService.getAllCars(pageable);
        if (currency != null) {
            cars.forEach(car -> applyPriceConversion(car, currency));
        }
        log.info("Successfully retrieved {} cars", cars.getTotalElements());
        return ResponseEntity.ok(cars);
    }

    @GetMapping(value = "/active")
    @Operation(summary = "Get active cars", description = "Retrieve all active (not deleted) cars with pagination and optional currency conversion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active cars retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters or currency")
    })
    public ResponseEntity<Page<CarResponseDto>> getAllActiveCars(
            @Parameter(description = "Pagination information") @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "Target currency for price conversion") @RequestParam(required = false) CurrencyType currency) {
        Page<CarResponseDto> cars = carService.getAllActiveCars(pageable);
        if (currency != null) {
            cars.forEach(car -> applyPriceConversion(car, currency));
        }
        return ResponseEntity.ok(cars);
    }

    private void applyPriceConversion(CarResponseDto car, CurrencyType targetCurrency) {
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
