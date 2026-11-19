package com.akif.rental.web;

import com.akif.shared.security.AuthenticatedUser;
import com.akif.shared.web.AllowedSort;
import com.akif.currency.api.DisplayPrice;
import com.akif.payment.api.PaymentIntentResult;
import com.akif.payment.api.PaymentSummary;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.internal.dto.request.ApproveRequest;
import com.akif.rental.internal.dto.request.CancelRequest;
import com.akif.rental.internal.dto.request.PickupRequest;
import com.akif.rental.api.RentalRequest;
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
import com.akif.shared.web.PageResponse;
import org.springframework.data.domain.Page;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rentals")
@RequiredArgsConstructor
@Tag(name = "Rental Management", description = "Operations for managing car rentals")
public class RentalController {

        private final RentalService rentalService;
        private final CurrencyConversionService currencyConversionService;

        @PostMapping("/request")
        @PreAuthorize("hasRole('USER')")
        @Operation(summary = "Request a rental", description = "Create a new rental request")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Rental request created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RentalResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid rental data or car not rentable (CAR_NOT_AVAILABLE)"),
                        @ApiResponse(responseCode = "404", description = "Car not found"),
                        @ApiResponse(responseCode = "409", description = "Dates overlap an existing rental (CAR_DATES_UNAVAILABLE)")
        })
        public ResponseEntity<RentalResponse> requestRental(
                        @Parameter(description = "Rental request data", required = true) @Valid @RequestBody RentalRequest request,
                        @AuthenticationPrincipal AuthenticatedUser caller) {


                RentalResponse rental = rentalService.requestRental(request, caller.id());

                return ResponseEntity.status(HttpStatus.CREATED).body(rental);
        }

        @PostMapping("/{id}/confirm")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Confirm rental", description = "Admin confirms a rental request and authorizes payment")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Rental confirmed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RentalResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid rental state"),
                        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
                        @ApiResponse(responseCode = "404", description = "Rental not found"),
                        @ApiResponse(responseCode = "402", description = "Payment failed")
        })
        public ResponseEntity<RentalResponse> confirmRental(
                        @Parameter(description = "Rental ID", required = true) @PathVariable Long id,
                        @RequestBody(required = false) ApproveRequest request) {


                String notes = (request != null) ? request.notes() : null;
                RentalResponse rental = rentalService.confirmRental(id, notes);

                return ResponseEntity.ok(rental);
        }

        @PostMapping("/{id}/pickup")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Pickup rental", description = "Admin processes car pickup and captures payment")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Pickup processed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RentalResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid rental state"),
                        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
                        @ApiResponse(responseCode = "404", description = "Rental not found")
        })
        public ResponseEntity<RentalResponse> pickupRental(
                        @Parameter(description = "Rental ID", required = true) @PathVariable Long id,
                        @Parameter(description = "Pickup notes") @RequestBody(required = false) PickupRequest request) {


                String notes = (request != null) ? request.notes() : null;
                RentalResponse rental = rentalService.pickupRental(id, notes);

                return ResponseEntity.ok(rental);
        }

        @PostMapping("/{id}/return")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Return rental", description = "Admin processes car return")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Return processed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RentalResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid rental state"),
                        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
                        @ApiResponse(responseCode = "404", description = "Rental not found")
        })
        public ResponseEntity<RentalResponse> returnRental(
                        @Parameter(description = "Rental ID", required = true) @PathVariable Long id,
                        @Parameter(description = "Return notes") @RequestBody(required = false) ReturnRequest request) {


                String notes = (request != null) ? request.notes() : null;
                RentalResponse rental = rentalService.returnRental(id, notes);

                return ResponseEntity.ok(rental);
        }

        @PostMapping("/{id}/early-return")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Early return", description = "Admin returns the car before the end date; the unused days are refunded partially through the normal refund path. Late and damage handling are unchanged.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Early return processed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RentalResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid rental state"),
                        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
                        @ApiResponse(responseCode = "404", description = "Rental not found")
        })
        public ResponseEntity<RentalResponse> earlyReturnRental(
                        @Parameter(description = "Rental ID", required = true) @PathVariable Long id,
                        @Parameter(description = "Return notes") @RequestBody(required = false) ReturnRequest request) {


                String notes = (request != null) ? request.notes() : null;
                RentalResponse rental = rentalService.earlyReturnRental(id, notes);

                return ResponseEntity.ok(rental);
        }

        @PostMapping("/{id}/cancel")
        @Operation(summary = "Cancel rental", description = "Cancel a REQUESTED or CONFIRMED rental (user can cancel own, admin can cancel any). A rental in use ends only through the return flow.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Rental cancelled successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RentalResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Rental cannot be cancelled in its current status (RENTAL_CANCEL_NOT_ALLOWED)"),
                        @ApiResponse(responseCode = "404", description = "Rental not found or not owned by the caller")
        })
        public ResponseEntity<RentalResponse> cancelRental(
                        @Parameter(description = "Rental ID", required = true) @PathVariable Long id,
                        @RequestBody(required = false) CancelRequest request,
                        @AuthenticationPrincipal AuthenticatedUser caller) {


                String reason = (request != null) ? request.reason() : "Cancelled by user";
                RentalResponse rental = rentalService.cancelRental(id, caller.id(), reason);

                return ResponseEntity.ok(rental);
        }

        @PostMapping("/{id}/payment-intent")
        @PreAuthorize("hasRole('USER')")
        @Operation(summary = "Start or resume payment", description = "Creates the Stripe PaymentIntent for a confirmed rental (idempotent) and returns the client secret for Stripe Elements. The amount is always computed by the server.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Payment intent ready", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentIntentResult.class))),
                        @ApiResponse(responseCode = "400", description = "Rental is not confirmed"),
                        @ApiResponse(responseCode = "404", description = "Rental not found or not owned by the caller"),
                        @ApiResponse(responseCode = "409", description = "Payment already settled")
        })
        public ResponseEntity<PaymentIntentResult> createPaymentIntent(
                        @Parameter(description = "Rental ID", required = true) @PathVariable Long id,
                        @AuthenticationPrincipal AuthenticatedUser caller) {

                return ResponseEntity.ok(rentalService.createPaymentIntent(id, caller.id()));
        }

        @GetMapping("/{id}/payment")
        @Operation(summary = "Get rental payment status", description = "Returns the payment status of a rental (owner or admin). Pending payments are refreshed from Stripe.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Payment status", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentSummary.class))),
                        @ApiResponse(responseCode = "404", description = "Rental not found, not owned by the caller or without a payment")
        })
        public ResponseEntity<PaymentSummary> getRentalPayment(
                        @Parameter(description = "Rental ID", required = true) @PathVariable Long id,
                        @AuthenticationPrincipal AuthenticatedUser caller) {

                return ResponseEntity.ok(rentalService.getRentalPayment(id, caller.id()));
        }

        @AllowedSort({"createTime", "startDate", "endDate", "totalPrice", "status"})
        @GetMapping("/me")
        @PreAuthorize("hasRole('USER')")
        @Operation(summary = "Get my rentals", description = "Get current user's rental list with optional currency conversion")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Rentals retrieved successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<PageResponse<RentalResponse>> getMyRentals(
                        @Parameter(description = "Pagination information") @PageableDefault(size = 20) @ParameterObject Pageable pageable,
                        @Parameter(description = "Target currency for price conversion") @RequestParam(required = false) CurrencyType currency,
                        @AuthenticationPrincipal AuthenticatedUser caller) {


                Page<RentalResponse> rentals = rentalService.getMyRentals(caller.id(), pageable);
                if (currency != null) {
                        rentals = rentals.map(rental -> applyPriceConversion(rental, currency));
                }

                return ResponseEntity.ok(PageResponse.of(rentals));
        }

        @AllowedSort({"createTime", "startDate", "endDate", "totalPrice", "status"})
        @GetMapping("/admin")
        @Operation(summary = "Get all rentals (Admin)", description = "Get all rentals (admin only) with optional currency conversion")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Rentals retrieved successfully"),
                        @ApiResponse(responseCode = "403", description = "Access denied")
        })
        public ResponseEntity<PageResponse<RentalResponse>> getAllRentals(
                        @Parameter(description = "Pagination information") @PageableDefault(size = 20) @ParameterObject Pageable pageable,
                        @Parameter(description = "Target currency for price conversion") @RequestParam(required = false) CurrencyType currency) {


                Page<RentalResponse> rentals = rentalService.getAllRentals(pageable);
                if (currency != null) {
                        rentals = rentals.map(rental -> applyPriceConversion(rental, currency));
                }

                return ResponseEntity.ok(PageResponse.of(rentals));
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get rental by ID", description = "Get rental details with optional currency conversion (user can view own, admin can view any)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Rental found successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RentalResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Rental not found or not owned by the caller")
        })
        public ResponseEntity<RentalResponse> getRentalById(
                        @Parameter(description = "Rental ID", required = true) @PathVariable Long id,
                        @Parameter(description = "Target currency for price conversion") @RequestParam(required = false) CurrencyType currency,
                        @AuthenticationPrincipal AuthenticatedUser caller) {


                RentalResponse rental = rentalService.getRentalById(id, caller.id());
                rental = applyPriceConversion(rental, currency);

                return ResponseEntity.ok(rental);
        }

        private RentalResponse applyPriceConversion(RentalResponse rental, CurrencyType targetCurrency) {
                return rental.withDisplayPrice(DisplayPrice.of(currencyConversionService, rental.totalPrice(),
                                rental.currency(), targetCurrency));
        }
}
