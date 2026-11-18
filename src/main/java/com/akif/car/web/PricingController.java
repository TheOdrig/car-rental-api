package com.akif.car.web;

import java.time.Clock;

import com.akif.car.internal.dto.response.PricingResponse;
import com.akif.car.internal.dto.pricing.PricingRequest;
import com.akif.car.api.DynamicPricingService;
import com.akif.car.internal.service.pricing.PricingStrategyCatalog;
import com.akif.car.api.PricingResult;
import com.akif.car.internal.service.pricing.PricingStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
@Tag(name = "Pricing", description = "Dynamic pricing management endpoints")
public class PricingController {

    private final Clock clock;

    private final DynamicPricingService dynamicPricingService;
    private final PricingStrategyCatalog pricingStrategyCatalog;

    @PostMapping("/calculate")
    @Operation(summary = "Calculate rental price", 
               description = "Calculates the final rental price with all applicable pricing strategies")
    public ResponseEntity<PricingResponse> calculatePrice(@Valid @RequestBody PricingRequest request) {
        log.info("Calculating price for car: {}, dates: {} to {}", 
            request.carId(), request.startDate(), request.endDate());

        PricingResult result = dynamicPricingService.calculatePrice(
            request.carId(),
            request.startDate(),
            request.endDate(),
            LocalDate.now(clock)
        );

        PricingResponse response = PricingResponse.fromPricingResult(result);
        
        log.info("Price calculated: base={}, final={}, savings={}", 
            response.getBaseTotalPrice(), response.getFinalPrice(), response.getTotalSavings());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/preview")
    @Operation(summary = "Preview rental price", 
               description = "Gets a price preview without creating a rental")
    public ResponseEntity<PricingResponse> previewPrice(
            @RequestParam Long carId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        log.info("Previewing price for car: {}, dates: {} to {}", carId, startDate, endDate);

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        PricingResult result = dynamicPricingService.previewPrice(carId, start, end);
        PricingResponse response = PricingResponse.fromPricingResult(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/strategies")
    @Operation(summary = "List enabled pricing strategies", 
               description = "Returns all currently enabled pricing strategies")
    public ResponseEntity<List<Map<String, Object>>> getEnabledStrategies() {
        log.info("Fetching enabled pricing strategies");

        List<PricingStrategy> strategies = pricingStrategyCatalog.getEnabledStrategies();
        
        List<Map<String, Object>> response = strategies.stream()
            .map(strategy -> Map.of(
                "name", (Object) strategy.getStrategyName(),
                "order", (Object) strategy.getOrder(),
                "enabled", (Object) strategy.isEnabled()
            ))
            .toList();

        log.info("Found {} enabled strategies", response.size());

        return ResponseEntity.ok(response);
    }
}
