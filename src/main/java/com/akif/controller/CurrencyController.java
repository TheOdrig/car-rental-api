package com.akif.controller;

import com.akif.dto.currency.ConversionResult;
import com.akif.dto.currency.ConvertRequest;
import com.akif.dto.currency.ExchangeRate;
import com.akif.dto.currency.ExchangeRatesResponse;
import com.akif.shared.enums.CurrencyType;
import com.akif.service.currency.ICurrencyConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Currency", description = "Currency conversion and exchange rate operations")
public class CurrencyController {

    private final ICurrencyConversionService currencyConversionService;

    @GetMapping
    @Operation(summary = "Get all exchange rates", description = "Returns current exchange rates for all supported currencies (USD base)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exchange rates retrieved successfully")
    })
    public ResponseEntity<ExchangeRatesResponse> getAllRates() {
        log.debug("Getting all exchange rates");
        ExchangeRatesResponse response = currencyConversionService.getAllRates();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{from}/{to}")
    @Operation(summary = "Get exchange rate between two currencies")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exchange rate retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid currency code")
    })
    public ResponseEntity<ExchangeRate> getRate(
            @Parameter(description = "Source currency code") @PathVariable CurrencyType from,
            @Parameter(description = "Target currency code") @PathVariable CurrencyType to) {
        log.debug("Getting exchange rate from {} to {}", from, to);
        ExchangeRate rate = currencyConversionService.getRate(from, to);
        return ResponseEntity.ok(rate);
    }


    @PostMapping("/convert")
    @Operation(summary = "Convert amount between currencies")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversion successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ConversionResult> convert(@Valid @RequestBody ConvertRequest request) {
        log.debug("Converting {} {} to {}", request.amount(), request.fromCurrency(), request.toCurrency());
        ConversionResult result = currencyConversionService.convert(
                request.amount(),
                request.fromCurrency(),
                request.toCurrency()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refresh exchange rates", description = "Forces a refresh of exchange rates from external API (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rates refreshed successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    public ResponseEntity<Void> refreshRates() {
        log.info("Admin requested exchange rate refresh");
        currencyConversionService.refreshRates();
        return ResponseEntity.ok().build();
    }
}
