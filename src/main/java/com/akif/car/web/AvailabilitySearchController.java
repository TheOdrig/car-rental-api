package com.akif.car.web;

import com.akif.car.internal.dto.availability.AvailabilitySearchRequest;
import com.akif.car.internal.dto.availability.AvailabilitySearchResponse;
import com.akif.car.internal.dto.availability.CarAvailabilityCalendarDto;
import com.akif.car.internal.dto.availability.SimilarCarDto;
import com.akif.car.internal.service.availability.CarAvailabilityService;
import com.akif.car.internal.service.availability.SimilarCarService;
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

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Availability Search", description = "Car availability search and calendar operations")
public class AvailabilitySearchController {

    private final CarAvailabilityService carAvailabilityService;
    private final SimilarCarService similarCarService;

    @PostMapping("/availability/search")
    @Operation(summary = "Search available cars", 
               description = "Search for cars available within a specific date range with optional filters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = AvailabilitySearchResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria or date range")
    })
    public ResponseEntity<AvailabilitySearchResponse> searchAvailableCars(
            @Parameter(description = "Availability search criteria with date range and filters", required = true)
            @Valid @RequestBody AvailabilitySearchRequest request) {
        log.debug("POST /api/cars/availability/search - Searching available cars for dates: {} to {}", 
                 request.getStartDate(), request.getEndDate());
        
        AvailabilitySearchResponse result = carAvailabilityService.searchAvailableCars(request);
        
        log.info("Availability search completed: found {} available cars for {} days", 
                result.totalElements(), result.rentalDays());
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/availability/calendar")
    @Operation(summary = "Get car availability calendar", 
               description = "Retrieve monthly availability calendar for a specific car showing available and unavailable dates")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Calendar retrieved successfully",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = CarAvailabilityCalendarDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid car ID or month format"),
            @ApiResponse(responseCode = "404", description = "Car not found")
    })
    public ResponseEntity<CarAvailabilityCalendarDto> getCarAvailabilityCalendar(
            @Parameter(description = "Car ID", required = true) 
            @PathVariable Long id,
            @Parameter(description = "Month in yyyy-MM format (default: current month)", example = "2025-01")
            @RequestParam(required = false) String month) {
        
        YearMonth targetMonth = month != null 
            ? YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"))
            : YearMonth.now();
        
        log.debug("GET /api/cars/{}/availability/calendar - Getting calendar for month: {}", id, targetMonth);
        
        CarAvailabilityCalendarDto calendar = carAvailabilityService.getCarAvailabilityCalendar(id, targetMonth);
        
        log.info("Successfully retrieved availability calendar for car: ID={}, month={}, blocked={}", 
                id, targetMonth, calendar.carBlocked());
        
        return ResponseEntity.ok(calendar);
    }

    @GetMapping("/{id}/similar")
    @Operation(summary = "Get similar available cars", 
               description = "Find similar cars that are available for the specified date range. If dates are not provided, defaults to today + 30 days.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Similar cars retrieved successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Invalid car ID or date parameters"),
            @ApiResponse(responseCode = "404", description = "Car not found")
    })
    public ResponseEntity<List<SimilarCarDto>> getSimilarAvailableCars(
            @Parameter(description = "Car ID", required = true) 
            @PathVariable Long id,
            @Parameter(description = "Rental start date (default: today)", example = "2025-01-15")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "Rental end date (default: today + 30 days)", example = "2025-01-20")
            @RequestParam(required = false) String endDate,
            @Parameter(description = "Maximum number of similar cars to return (default: 5, max: 10)")
            @RequestParam(defaultValue = "5") int limit) {
        
        LocalDate start = startDate != null 
            ? LocalDate.parse(startDate) 
            : LocalDate.now();
        LocalDate end = endDate != null 
            ? LocalDate.parse(endDate) 
            : LocalDate.now().plusDays(30);

        int effectiveLimit = Math.min(limit, 10);
        
        log.debug("GET /api/cars/{}/similar - Finding similar cars for dates: {} to {}, limit: {}", 
                 id, start, end, effectiveLimit);
        
        List<SimilarCarDto> similarCars = similarCarService.findSimilarAvailableCars(id, start, end, effectiveLimit);
        
        log.info("Found {} similar available cars for car: ID={}", similarCars.size(), id);
        
        return ResponseEntity.ok(similarCars);
    }
}
