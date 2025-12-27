package com.akif.rental.web;

import com.akif.currency.api.ConversionResult;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.internal.dto.request.PickupRequest;
import com.akif.rental.internal.dto.request.RentalRequest;
import com.akif.rental.internal.dto.request.ReturnRequest;
import com.akif.shared.enums.CurrencyType;
import com.akif.rental.api.RentalService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Rental Management", description = "Operations for managing car rentals")
public class RentalController {

    private final RentalService rentalService;
    private final CurrencyConversionService currencyConversionService;

    @PostMapping("/request")
    @Operation(summary = "Request a rental", description = "Create a new rental request")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rental request created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RentalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid rental data"),
            @ApiResponse(responseCode = "404", description = "Car not found"),
            @ApiResponse(responseCode = "409", description = "Date overlap or car not available")
    })
    public ResponseEntity<RentalResponse> requestRental(
            @Parameter(description = "Rental request data", required = true)
            @Valid @RequestBody RentalRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("POST /api/rentals/request - User: {}", username);

        RentalResponse rental = rentalService.requestRental(request, username);

        log.info("Rental request created successfully. RentalId: {}", rental.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(rental);
    }


    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Confirm rental", description = "Admin confirms a rental request and authorizes payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rental confirmed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RentalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid rental state"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
            @ApiResponse(responseCode = "404", description = "Rental not found"),
            @ApiResponse(responseCode = "402", description = "Payment failed")
    })
    public ResponseEntity<RentalResponse> confirmRental(
            @Parameter(description = "Rental ID", required = true)
            @PathVariable Long id) {

        log.info("POST /api/rentals/{}/confirm", id);

        RentalResponse rental = rentalService.confirmRental(id);

        log.info("Rental confirmed successfully. RentalId: {}", id);
        return ResponseEntity.ok(rental);
    }


    @PostMapping("/{id}/pickup")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Pickup rental", description = "Admin processes car pickup and captures payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pickup processed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RentalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid rental state"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
            @ApiResponse(responseCode = "404", description = "Rental not found")
    })
    public ResponseEntity<RentalResponse> pickupRental(
            @Parameter(description = "Rental ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Pickup notes")
            @RequestBody(required = false) PickupRequest request) {

        log.info("POST /api/rentals/{}/pickup", id);

        String notes = (request != null) ? request.notes() : null;
        RentalResponse rental = rentalService.pickupRental(id, notes);

        log.info("Pickup processed successfully. RentalId: {}", id);
        return ResponseEntity.ok(rental);
    }


    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Return rental", description = "Admin processes car return")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return processed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RentalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid rental state"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
            @ApiResponse(responseCode = "404", description = "Rental not found")
    })
    public ResponseEntity<RentalResponse> returnRental(
            @Parameter(description = "Rental ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Return notes")
            @RequestBody(required = false) ReturnRequest request) {

        log.info("POST /api/rentals/{}/return", id);

        String notes = (request != null) ? request.notes() : null;
        RentalResponse rental = rentalService.returnRental(id, notes);

        log.info("Return processed successfully. RentalId: {}", id);
        return ResponseEntity.ok(rental);
    }


    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel rental", description = "Cancel a rental (user can cancel own, admin can cancel any)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rental cancelled successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RentalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid rental state"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Rental not found")
    })
    public ResponseEntity<RentalResponse> cancelRental(
            @Parameter(description = "Rental ID", required = true)
            @PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("POST /api/rentals/{}/cancel - User: {}", id, username);

        RentalResponse rental = rentalService.cancelRental(id, username);

        log.info("Rental cancelled successfully. RentalId: {}", id);
        return ResponseEntity.ok(rental);
    }


    @GetMapping("/me")
    @Operation(summary = "Get my rentals", description = "Get current user's rental list with optional currency conversion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rentals retrieved successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<RentalResponse>> getMyRentals(
            @Parameter(description = "Pagination information")
            @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "Target currency for price conversion")
            @RequestParam(required = false) CurrencyType currency,
            Authentication authentication) {

        String username = authentication.getName();
        log.debug("GET /api/rentals/me - User: {}, currency: {}", username, currency);

        Page<RentalResponse> rentals = rentalService.getMyRentals(username, pageable);
        if (currency != null) {
            rentals = rentals.map(rental -> applyPriceConversion(rental, currency));
        }

        log.info("Retrieved {} rentals for user: {}", rentals.getTotalElements(), username);
        return ResponseEntity.ok(rentals);
    }


    @GetMapping("/admin")
    @Operation(summary = "Get all rentals (Admin)", description = "Get all rentals (admin only) with optional currency conversion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rentals retrieved successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Page<RentalResponse>> getAllRentals(
            @Parameter(description = "Pagination information")
            @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "Target currency for price conversion")
            @RequestParam(required = false) CurrencyType currency) {

        log.debug("GET /api/rentals/admin with currency: {}", currency);

        Page<RentalResponse> rentals = rentalService.getAllRentals(pageable);
        if (currency != null) {
            rentals = rentals.map(rental -> applyPriceConversion(rental, currency));
        }

        log.info("Retrieved {} rentals", rentals.getTotalElements());
        return ResponseEntity.ok(rentals);
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get rental by ID", description = "Get rental details with optional currency conversion (user can view own, admin can view any)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rental found successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RentalResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Rental not found")
    })
    public ResponseEntity<RentalResponse> getRentalById(
            @Parameter(description = "Rental ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Target currency for price conversion")
            @RequestParam(required = false) CurrencyType currency,
            Authentication authentication) {

        String username = authentication.getName();
        log.debug("GET /api/rentals/{} - User: {}, currency: {}", id, username, currency);

        RentalResponse rental = rentalService.getRentalById(id, username);
        rental = applyPriceConversion(rental, currency);

        log.info("Retrieved rental: {}", id);
        return ResponseEntity.ok(rental);
    }

    private RentalResponse applyPriceConversion(RentalResponse rental, CurrencyType targetCurrency) {
        if (targetCurrency == null || rental.totalPrice() == null) {
            return rental;
        }

        CurrencyType originalCurrency = rental.currency() != null ? rental.currency() : CurrencyType.TRY;

        if (originalCurrency.equals(targetCurrency)) {
            return new RentalResponse(
                rental.id(),
                rental.carSummary(),
                rental.userSummary(),
                rental.startDate(),
                rental.endDate(),
                rental.days(),
                rental.dailyPrice(),
                rental.totalPrice(),
                rental.currency(),
                rental.status(),
                rental.originalPrice(),
                rental.finalPrice(),
                rental.totalSavings(),
                rental.appliedDiscounts(),
                rental.totalPrice(),
                targetCurrency,
                java.math.BigDecimal.ONE,
                "LIVE",
                rental.pickupNotes(),
                rental.returnNotes(),
                rental.createTime(),
                rental.updateTime()
            );
        }

        ConversionResult result = currencyConversionService.convert(
                rental.totalPrice(), originalCurrency, targetCurrency);
        
        return new RentalResponse(
            rental.id(),
            rental.carSummary(),
            rental.userSummary(),
            rental.startDate(),
            rental.endDate(),
            rental.days(),
            rental.dailyPrice(),
            rental.totalPrice(),
            rental.currency(),
            rental.status(),
            rental.originalPrice(),
            rental.finalPrice(),
            rental.totalSavings(),
            rental.appliedDiscounts(),
            result.convertedAmount(),
            targetCurrency,
            result.exchangeRate(),
            result.source().getDisplayName(),
            rental.pickupNotes(),
            rental.returnNotes(),
            rental.createTime(),
            rental.updateTime()
        );
    }
}
