package com.akif.rental.internal.service;

import com.akif.shared.time.UtcDates;
import java.time.Clock;
import com.akif.car.api.CarDatesUnavailableException;
import com.akif.payment.api.PaymentFailedException;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.api.EarlyReturnPreview;
import com.akif.payment.api.PaymentDto;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.api.CreatePaymentRequest;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.internal.config.RentalProperties;
import com.akif.rental.internal.exception.InvalidRentalStateException;
import com.akif.rental.internal.exception.RentalCancelNotAllowedException;
import com.akif.rental.internal.exception.PaymentNotAuthorizedException;
import com.akif.rental.internal.exception.RentalNotFoundException;
import com.akif.rental.internal.exception.RentalValidationException;
import com.akif.rental.api.RentalCancelledEvent;
import com.akif.rental.api.RentalConfirmedEvent;

import com.akif.rental.domain.model.Rental;
import com.akif.auth.api.UserDto;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.payment.api.PaymentService;
import com.akif.payment.api.PaymentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RentalLifecycleService {

    private static final String CARD_PAYMENT_METHOD = "CARD";

    private final Clock clock;
    private final RentalRepository rentalRepository;
    private final RentalAssembler assembler;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher eventPublisher;
    private final RentalProperties rentalProperties;
    private final RentalLookups lookups;
    private final RentalRefundService refunds;
    private final RentalLatePenaltyProcessor penalties;

    @Transactional
    public RentalResponse confirmRental(Long rentalId, String notes) {
        log.info("Confirming rental: {}", rentalId);

        Rental rental = lookups.findRentalById(rentalId);

        if (!rental.getStatus().canConfirm()) {
            throw new InvalidRentalStateException(
                    rental.getStatus().name(),
                    RentalStatus.REQUESTED.name());
        }

        lookups.requireCarAvailable(rental.getCarId());

        PaymentDto paymentDto = paymentService.createPayment(
                new CreatePaymentRequest(
                        rental.getId(),
                        rental.getUserEmail(),
                        rental.getCarLicensePlate(),
                        rental.getTotalPrice(),
                        rental.getCurrency(),
                        CARD_PAYMENT_METHOD));

        rental.confirm(notes, Instant.now(clock));

        Rental updatedRental = rentalRepository.save(rental);
        RentalResponse result = assembler.toResponse(updatedRental, paymentDto.status());

        RentalConfirmedEvent event = new RentalConfirmedEvent(
                updatedRental.getId(),
                updatedRental.getUserEmail(),
                Instant.now(clock),
                updatedRental.getCarBrand(),
                updatedRental.getCarModel(),
                updatedRental.getStartDate(),
                updatedRental.getEndDate(),
                updatedRental.getTotalPrice(),
                updatedRental.getCurrency(),
                notes);
        eventPublisher.publishEvent(event);
        log.info("Published RentalConfirmedEvent for rental: {}", updatedRental.getId());

        lookups.logRentalOperationSuccess("confirmed", result, "PaymentId: " + paymentDto.id());
        return result;
    }

    @Transactional
    public RentalResponse pickupRental(Long rentalId, String pickupNotes) {
        log.info("Processing pickup for rental: {}", rentalId);

        Rental rental = lookups.findRentalForUpdate(rentalId);

        if (!rental.getStatus().canPickup()) {
            throw new InvalidRentalStateException(
                    rental.getStatus().name(),
                    RentalStatus.CONFIRMED.name());
        }

        if (LocalDate.now(clock).isBefore(rental.getStartDate())) {
            throw new RentalValidationException("Pickup is not allowed before the start date " + rental.getStartDate());
        }

        LocalDate lastPickupDay = rental.getStartDate().plusDays(rentalProperties.getNoShowDays() - 1L);
        if (LocalDate.now(clock).isAfter(lastPickupDay)) {
            throw new RentalValidationException("Pickup is not allowed after " + lastPickupDay
                    + ", the booking is released as a no-show");
        }
        if (LocalDate.now(clock).isAfter(rental.getEndDate())) {
            throw new RentalValidationException("Pickup is not allowed after the end date " + rental.getEndDate());
        }

        lookups.requireCarAvailable(rental.getCarId());
        if (rentalRepository.existsByCarIdAndStatusAndIsDeletedFalseAndIdNot(
                rental.getCarId(), RentalStatus.IN_USE, rental.getId())) {
            throw CarDatesUnavailableException.stillInUse(rental.getCarId());
        }

        PaymentDto payment = lookups.findPaymentByRentalId(rentalId);
        if (payment.status() != PaymentStatus.AUTHORIZED && payment.status() != PaymentStatus.CAPTURED) {
            throw new PaymentNotAuthorizedException(payment.status().name());
        }

        PaymentResult captureResult = paymentService.capturePayment(payment.id());

        if (!captureResult.success()) {
            throw new PaymentFailedException(
                    payment.transactionId(),
                    "Payment capture failed: " + captureResult.message());
        }

        rental.pickUp(pickupNotes);
        Rental updatedRental = rentalRepository.save(rental);
        RentalResponse result = assembler.toResponse(updatedRental);

        lookups.logRentalOperationSuccess("picked up", result, "Notes: " + (pickupNotes != null ? pickupNotes : "None"));
        return result;
    }

    @Transactional
    public RentalResponse returnRental(Long rentalId, String returnNotes) {
        return completeReturn(rentalId, returnNotes, false);
    }

    @Transactional
    public RentalResponse earlyReturnRental(Long rentalId, String returnNotes) {
        return completeReturn(rentalId, returnNotes, true);
    }

    public EarlyReturnPreview previewEarlyReturn(Long rentalId) {
        Rental rental = lookups.findRentalById(rentalId);

        if (!rental.getStatus().canReturn()) {
            throw new InvalidRentalStateException(
                    rental.getStatus().name(),
                    RentalStatus.IN_USE.name());
        }

        return refunds.previewEarlyReturn(rental);
    }

    private RentalResponse completeReturn(Long rentalId, String returnNotes, boolean refundUnusedDays) {
        log.info("Processing return for rental: {} (refundUnusedDays={})", rentalId, refundUnusedDays);

        Rental rental = lookups.findRentalForUpdate(rentalId);

        if (!rental.getStatus().canReturn()) {
            throw new InvalidRentalStateException(
                    rental.getStatus().name(),
                    RentalStatus.IN_USE.name());
        }

        Instant actualReturnTime = Instant.now(clock);

        if (actualReturnTime.isAfter(UtcDates.endOfDayWholeSecond(rental.getEndDate()))) {
            log.info("Processing late return penalty for rental: {}", rentalId);
            penalties.processPenaltyForLateReturn(rental, actualReturnTime);
        }

        if (refundUnusedDays) {
            refunds.refundPartialPayment(rental);
        }

        rental.markReturned(actualReturnTime, returnNotes);

        Rental updatedRental = rentalRepository.save(rental);
        RentalResponse result = assembler.toResponse(updatedRental);

        lookups.logRentalOperationSuccess("returned", result, "Notes: " + (returnNotes != null ? returnNotes : "None"));
        return result;
    }

    @Transactional
    public RentalResponse cancelRental(Long rentalId, Long userId, String reason) {
        log.info("Cancelling rental: {} by user: {}", rentalId, userId);

        Rental rental = lookups.findRentalForUpdate(rentalId);
        UserDto currentUser = lookups.findUserById(userId);

        if (!currentUser.isAdmin() && !rental.getUserId().equals(currentUser.id())) {
            throw new RentalNotFoundException(rentalId);
        }

        if (!rental.getStatus().canCancel()) {
            throw new RentalCancelNotAllowedException(rental.getStatus().name());
        }

        RentalStatus currentStatus = rental.getStatus();

        RefundInfo refundInfo = new RefundInfo(false, BigDecimal.ZERO, null);
        if (currentStatus == RentalStatus.CONFIRMED) {
            refundInfo = refunds.refundPayment(rentalId);
        }

        rental.cancel(reason);

        Rental updatedRental = rentalRepository.save(rental);
        RentalResponse result = assembler.toResponse(updatedRental);

        RentalCancelledEvent event = new RentalCancelledEvent(
                updatedRental.getId(),
                updatedRental.getUserEmail(),
                Instant.now(clock),
                Instant.now(clock),
                "Cancelled by " + (currentUser.isAdmin() ? "admin" : "customer") + ". Reason: " + reason,
                refundInfo.refundProcessed(),
                refundInfo.refundAmount(),
                refundInfo.refundTransactionId());
        eventPublisher.publishEvent(event);
        log.info("Published RentalCancelledEvent for rental: {}", updatedRental.getId());

        lookups.logRentalOperationSuccess("cancelled", result, "By user: " + userId);
        return result;
    }

    @Transactional
    public void incrementDamageReportCount(Long rentalId) {
        log.debug("Incrementing damage report count for rental: {}", rentalId);

        Rental rental = lookups.findRentalById(rentalId);
        rental.registerDamageReport();

        rentalRepository.save(rental);
        log.info("Incremented damage report count for rental: {} (new count: {})",
                rentalId, rental.getDamageReportsCount());
    }
}
