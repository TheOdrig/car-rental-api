package com.akif.car.web;

import com.akif.car.internal.dto.request.CarRequest;
import com.akif.car.internal.dto.response.CarStatisticsResponse;
import com.akif.car.internal.service.CarQueryService;
import com.akif.car.internal.service.CarCommandService;
import com.akif.car.internal.service.CarStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cars/statistics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Car Statistics", description = "Statistics and analytics for cars")
public class CarStatisticsController {

    private final CarQueryService carQueryService;
    private final CarCommandService carCommandService;
    private final CarStatisticsService carStatisticsService;

    @GetMapping()
    @Operation(summary = "Get car statistics", description = "Retrieve comprehensive car statistics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CarStatisticsResponse> getCarStatistics() {
        CarStatisticsResponse statistics = carStatisticsService.getCarStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping(value = "/status-counts")
    @Operation(summary = "Get cars count by status", description = "Retrieve count of cars by status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status counts retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Long>> getCarsCountByStatus() {
        Map<String, Long> statusCounts = carStatisticsService.getCarsCountByStatus();
        return ResponseEntity.ok(statusCounts);
    }

    @GetMapping(value = "/brand-counts")
    @Operation(summary = "Get cars count by brand", description = "Retrieve count of cars by brand")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Brand counts retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Long>> getCarsCountByBrand() {
        Map<String, Long> brandCounts = carStatisticsService.getCarsCountByBrand();
        return ResponseEntity.ok(brandCounts);
    }

    @GetMapping(value = "/average-prices")
    @Operation(summary = "Get average prices by brand", description = "Retrieve average prices by brand")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Average prices retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, BigDecimal>> getAveragePriceByBrand() {
        Map<String, BigDecimal> averagePrices = carStatisticsService.getAveragePriceByBrand();
        return ResponseEntity.ok(averagePrices);
    }

    @GetMapping(value = "/count")
    @Operation(summary = "Get car count", description = "Get total number of cars")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Long>> getCarCount() {
        long count = carQueryService.getCarCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping(value = "/count/active")
    @Operation(summary = "Get active car count", description = "Get number of active cars")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active count retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Long>> getActiveCarCount() {
        long count = carQueryService.getActiveCarCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping(value = "/validate")
    @Operation(summary = "Validate car data", description = "Validate car data without saving")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Validation completed"),
            @ApiResponse(responseCode = "400", description = "Invalid car data")
    })
    public ResponseEntity<Map<String, Object>> validateCarData(
            @Parameter(description = "Car data to validate", required = true) @Valid @RequestBody CarRequest carRequest) {
        log.info("POST /api/v1/cars/statistics/validate");

        List<String> errors = carCommandService.validateCarData(carRequest);
        boolean isValid = errors.isEmpty();

        Map<String, Object> result = Map.of(
                "isValid", isValid,
                "errors", errors
        );

        return ResponseEntity.ok(result);
    }
}
