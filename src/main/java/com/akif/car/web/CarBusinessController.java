package com.akif.car.web;

import com.akif.car.api.CarResponse;
import com.akif.car.internal.dto.pricing.CarPriceUpdateRequest;
import com.akif.car.internal.dto.request.CarStatusUpdateRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cars/business")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Car Business", description = "Business operations for cars")
public class CarBusinessController {

    private final CarService carService;

    @PostMapping(value = "/{id}/reserve")
    @Operation(summary = "Reserve car", description = "Reserve a car for a customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Car reserved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarResponse.class))),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Car cannot be reserved")
    })
    public ResponseEntity<CarResponse> reserveCar(
            @Parameter(description = "Car ID", required = true) @PathVariable Long id) {
        CarResponse car = carService.reserveCar(id);
        return ResponseEntity.ok(car);
    }

    @PostMapping(value = "/{id}/cancel-reservation")
    @Operation(summary = "Cancel reservation", description = "Cancel car reservation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation cancelled successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarResponse.class))),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Car is not reserved")
    })
    public ResponseEntity<CarResponse> cancelReservation(
            @Parameter(description = "Car ID", required = true) @PathVariable Long id) {
        CarResponse car = carService.cancelReservation(id);
        return ResponseEntity.ok(car);
    }

    @PostMapping(value = "/{id}/maintenance")
    @Operation(summary = "Mark car as maintenance", description = "Mark a car as under maintenance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Car marked as maintenance successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarResponse.class))),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Invalid car ID")
    })
    public ResponseEntity<CarResponse> markAsMaintenance(
            @Parameter(description = "Car ID", required = true) @PathVariable Long id) {
        CarResponse car = carService.markAsMaintenance(id);
        return ResponseEntity.ok(car);
    }

    @PostMapping(value = "/{id}/available")
    @Operation(summary = "Mark car as available", description = "Mark a car as available for sale")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Car marked as available successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarResponse.class))),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Invalid car ID")
    })
    public ResponseEntity<CarResponse> markAsAvailable(
            @Parameter(description = "Car ID", required = true) @PathVariable Long id) {
        CarResponse car = carService.markAsAvailable(id);
        return ResponseEntity.ok(car);
    }

    @PatchMapping(value = "/{id}/status")
    @Operation(summary = "Update car status", description = "Update car status with reason")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Car status updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarResponse.class))),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition")
    })
    public ResponseEntity<CarResponse> updateCarStatus(
            @Parameter(description = "Car ID", required = true) @PathVariable Long id,
            @Parameter(description = "Status update data", required = true) @Valid @RequestBody CarStatusUpdateRequest statusUpdateRequest) {
        CarResponse car = carService.updateCarStatus(id, statusUpdateRequest);
        return ResponseEntity.ok(car);
    }

    @PatchMapping(value = "/{id}/price")
    @Operation(summary = "Update car price", description = "Update car price and currency")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Car price updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CarResponse.class))),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Invalid price data")
    })
    public ResponseEntity<CarResponse> updateCarPrice(
            @Parameter(description = "Car ID", required = true) @PathVariable Long id,
            @Parameter(description = "Price update data", required = true) @Valid @RequestBody CarPriceUpdateRequest priceUpdateRequest) {
        CarResponse car = carService.updateCarPrice(id, priceUpdateRequest);
        return ResponseEntity.ok(car);
    }

    @GetMapping(value = "/{id}/can-reserve")
    @Operation(summary = "Check if car can be reserved", description = "Check if car can be reserved")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Check completed"),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "400", description = "Invalid car ID")
    })
    public ResponseEntity<Map<String, Boolean>> canCarBeReserved(
            @Parameter(description = "Car ID", required = true) @PathVariable Long id) {
        log.info("GET /api/v1/cars/business/{}/can-reserve", id);
        boolean canBeReserved = carService.canCarBeReserved(id);
        return ResponseEntity.ok(Map.of("canBeReserved", canBeReserved));
    }
}
