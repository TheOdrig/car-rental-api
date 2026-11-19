package com.akif.rental.internal.service;

import com.akif.shared.web.Pageables;
import java.time.Clock;

import com.akif.car.api.CarDto;
import com.akif.car.api.CarStatusType;
import com.akif.car.api.CarDatesUnavailableException;
import com.akif.car.api.CarNotAvailableException;
import com.akif.payment.api.PaymentFailedException;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.api.RentalSummaryDto;
import com.akif.rental.domain.enums.LateReturnStatus;
import com.akif.payment.api.PaymentDto;
import com.akif.payment.api.PaymentIntentResult;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.api.PaymentSummary;
import com.akif.payment.api.CreatePaymentRequest;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.internal.config.RentalProperties;
import com.akif.rental.internal.exception.InvalidRentalStateException;
import com.akif.rental.internal.exception.RentalCancelNotAllowedException;
import com.akif.rental.internal.exception.OpenRentalRequestLimitException;
import com.akif.rental.internal.exception.PaymentWindowNotOpenException;
import com.akif.rental.internal.exception.PaymentNotAuthorizedException;
import com.akif.rental.internal.exception.RentalPaymentNotFoundException;
import com.akif.rental.internal.exception.RentalNotFoundException;
import com.akif.rental.internal.exception.RentalValidationException;
import com.akif.rental.internal.dto.penalty.PenaltyResult;
import com.akif.rental.api.PenaltySummaryEvent;
import com.akif.rental.api.RentalCancelledEvent;
import com.akif.rental.api.RentalConfirmedEvent;
import com.akif.rental.api.RentalRequest;
import com.akif.rental.internal.mapper.RentalMapper;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.domain.model.RentalDatePolicy;
import com.akif.car.api.CarCatalog;
import com.akif.auth.api.UserLookup;
import com.akif.auth.api.UserDto;
import com.akif.rental.internal.service.penalty.PenaltyCalculationService;
import com.akif.rental.internal.service.penalty.PenaltyPaymentService;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.payment.api.PaymentService;
import com.akif.rental.api.RentalService;
import com.akif.auth.api.UserRentalStatisticsDto;
import com.akif.payment.api.PaymentResult;
import com.akif.car.api.DynamicPricingService;
import com.akif.car.api.PriceModifier;
import com.akif.car.api.PricingResult;
import com.akif.shared.enums.*;
import com.akif.shared.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.sql.SQLException;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RentalServiceImpl implements RentalService {

    private final Clock clock;

    private static final String CARD_PAYMENT_METHOD = "CARD";
    private static final String EXCLUSION_VIOLATION = "23P01";

    private final RentalRepository rentalRepository;

    private final CarCatalog carCatalog;
    private final UserLookup userLookup;

    private final PaymentService paymentService;
    private final RentalMapper rentalMapper;
    private final DynamicPricingService dynamicPricingService;
    private final ApplicationEventPublisher eventPublisher;
    private final PenaltyCalculationService penaltyCalculationService;
    private final PenaltyPaymentService penaltyPaymentService;
    private final RentalProperties rentalProperties;

    @Override
    @Transactional
    public RentalResponse requestRental(RentalRequest request, String username) {
        log.info("Creating rental request for user: {}, car: {}", username, request.carId());

        UserDto user = findUserByUsername(username);
        CarDto car = findCarById(request.carId());

        if (!car.status().equals(CarStatusType.AVAILABLE)) {
            throw new CarNotAvailableException(
                    car.id(),
                    "Car status is: " + car.status().getDisplayName());
        }

        validateRentalDates(request.startDate(), request.endDate());
        checkOpenRequestLimit(user.id());
        checkDateOverlap(car.id(), request.startDate(), request.endDate());

        PricingResult pricingResult = dynamicPricingService.calculatePrice(
                request.carId(),
                request.startDate(),
                request.endDate(),
                LocalDate.now(clock));

        int days = pricingResult.rentalDays();
        BigDecimal dailyPrice = pricingResult.effectiveDailyPrice();
        BigDecimal totalPrice = pricingResult.finalPrice();
        CurrencyType currency = car.currency();

        log.info("Dynamic pricing applied: base={}, final={}, modifiers={}",
                pricingResult.baseTotalPrice(), pricingResult.finalPrice(), pricingResult.appliedModifiers().size());

        Rental rental = Rental.builder()
                .userId(user.id())
                .carId(car.id())
                .carBrand(car.brand())
                .carModel(car.model())
                .carLicensePlate(car.licensePlate())
                .carThumbnailUrl(car.thumbnailUrl())
                .userEmail(user.email())
                .userFullName(user.firstName() + " " + user.lastName())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .days(days)
                .dailyPrice(dailyPrice)
                .totalPrice(totalPrice)
                .currency(currency)
                .status(RentalStatus.REQUESTED)
                .build();

        Rental savedRental = saveBooking(rental);
        RentalResponse result = rentalMapper.toDto(savedRental);

        result = new RentalResponse(
                result.id(),
                result.carSummary(),
                result.userSummary(),
                result.startDate(),
                result.endDate(),
                result.days(),
                result.dailyPrice(),
                result.totalPrice(),
                result.currency(),
                result.status(),
                pricingResult.baseTotalPrice(),
                pricingResult.finalPrice(),
                pricingResult.totalSavings(),
                pricingResult.appliedModifiers().stream()
                        .map(PriceModifier::description)
                        .toList(),
                result.convertedTotalPrice(),
                result.displayCurrency(),
                result.exchangeRate(),
                result.rateSource(),
                result.pickupNotes(),
                result.returnNotes(),
                result.approvalNotes(),
                result.cancellationReason(),
                result.createTime(),
                result.updateTime());

        logRentalOperationSuccess("created", result);
        return result;
    }

    private void validateRentalDates(LocalDate startDate, LocalDate endDate) {
        new RentalDatePolicy(rentalProperties.getMaxLeadDays(), rentalProperties.getMaxRentalDays())
                .validate(startDate, endDate, LocalDate.now(clock));
    }

    private void checkOpenRequestLimit(Long userId) {
        rentalRepository.acquireAdvisoryTransactionLock("rental-open-requests:" + userId);
        long open = rentalRepository.countByUserIdAndStatusAndIsDeletedFalse(userId, RentalStatus.REQUESTED);
        if (open >= rentalProperties.getMaxOpenRequestsPerUser()) {
            throw new OpenRentalRequestLimitException(rentalProperties.getMaxOpenRequestsPerUser());
        }
    }

    private void checkDateOverlap(Long carId, LocalDate startDate, LocalDate endDate) {
        long overlappingCount = rentalRepository.countOverlappingRentals(carId, startDate, endDate);
        if (overlappingCount > 0) {
            throw CarDatesUnavailableException.bookedForDates(carId, startDate, endDate);
        }
    }

    private Rental saveBooking(Rental rental) {
        try {
            return rentalRepository.saveAndFlush(rental);
        } catch (DataIntegrityViolationException e) {
            if (isExclusionViolation(e)) {
                throw CarDatesUnavailableException.bookedForDates(
                        rental.getCarId(), rental.getStartDate(), rental.getEndDate());
            }
            throw e;
        }
    }

    private static boolean isExclusionViolation(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException && EXCLUSION_VIOLATION.equals(sqlException.getSQLState())) {
                return true;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public RentalResponse confirmRental(Long rentalId, String notes) {
        log.info("Confirming rental: {}", rentalId);

        Rental rental = findRentalById(rentalId);

        if (!rental.getStatus().canConfirm()) {
            throw new InvalidRentalStateException(
                    rental.getStatus().name(),
                    RentalStatus.REQUESTED.name());
        }

        requireCarAvailable(rental.getCarId());

        PaymentDto paymentDto = paymentService.createPayment(
                new CreatePaymentRequest(
                        rental.getId(),
                        rental.getUserEmail(),
                        rental.getCarLicensePlate(),
                        rental.getTotalPrice(),
                        rental.getCurrency(),
                        CARD_PAYMENT_METHOD));

        rental.confirm(notes);

        Rental updatedRental = rentalRepository.save(rental);
        RentalResponse result = rentalMapper.toDto(updatedRental);

        RentalConfirmedEvent event = new RentalConfirmedEvent(
                updatedRental.getId(),
                updatedRental.getUserEmail(),
                LocalDateTime.now(clock),
                updatedRental.getCarBrand(),
                updatedRental.getCarModel(),
                updatedRental.getStartDate(),
                updatedRental.getEndDate(),
                updatedRental.getTotalPrice(),
                updatedRental.getCurrency(),
                notes);
        eventPublisher.publishEvent(event);
        log.info("Published RentalConfirmedEvent for rental: {}", updatedRental.getId());

        logRentalOperationSuccess("confirmed", result, "PaymentId: " + paymentDto.id());
        return result;
    }

    @Override
    @Transactional
    public RentalResponse pickupRental(Long rentalId, String pickupNotes) {
        log.info("Processing pickup for rental: {}", rentalId);

        Rental rental = findRentalForUpdate(rentalId);

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

        requireCarAvailable(rental.getCarId());
        if (rentalRepository.existsByCarIdAndStatusAndIsDeletedFalseAndIdNot(
                rental.getCarId(), RentalStatus.IN_USE, rental.getId())) {
            throw CarDatesUnavailableException.stillInUse(rental.getCarId());
        }

        PaymentDto payment = findPaymentByRentalId(rentalId);
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
        RentalResponse result = rentalMapper.toDto(updatedRental);

        logRentalOperationSuccess("picked up", result, "Notes: " + (pickupNotes != null ? pickupNotes : "None"));
        return result;
    }

    @Override
    @Transactional
    public RentalResponse returnRental(Long rentalId, String returnNotes) {
        return completeReturn(rentalId, returnNotes, false);
    }

    @Override
    @Transactional
    public RentalResponse earlyReturnRental(Long rentalId, String returnNotes) {
        return completeReturn(rentalId, returnNotes, true);
    }

    private RentalResponse completeReturn(Long rentalId, String returnNotes, boolean refundUnusedDays) {
        log.info("Processing return for rental: {} (refundUnusedDays={})", rentalId, refundUnusedDays);

        Rental rental = findRentalForUpdate(rentalId);

        if (!rental.getStatus().canReturn()) {
            throw new InvalidRentalStateException(
                    rental.getStatus().name(),
                    RentalStatus.IN_USE.name());
        }

        LocalDateTime actualReturnTime = LocalDateTime.now(clock);

        if (actualReturnTime.isAfter(rental.getEndDate().atTime(23, 59, 59))) {
            log.info("Processing late return penalty for rental: {}", rentalId);
            processPenaltyForLateReturn(rental, actualReturnTime);
        }

        if (refundUnusedDays) {
            refundPartialPayment(rental);
        }

        rental.markReturned(actualReturnTime, returnNotes);

        Rental updatedRental = rentalRepository.save(rental);
        RentalResponse result = rentalMapper.toDto(updatedRental);

        logRentalOperationSuccess("returned", result, "Notes: " + (returnNotes != null ? returnNotes : "None"));
        return result;
    }

    @Override
    @Transactional
    public RentalResponse cancelRental(Long rentalId, String username, String reason) {
        log.info("Cancelling rental: {} by user: {}", rentalId, username);

        Rental rental = findRentalForUpdate(rentalId);
        UserDto currentUser = findUserByUsername(username);

        if (!currentUser.isAdmin() && !rental.getUserId().equals(currentUser.id())) {
            throw new RentalNotFoundException(rentalId);
        }

        if (!rental.getStatus().canCancel()) {
            throw new RentalCancelNotAllowedException(rental.getStatus().name());
        }

        RentalStatus currentStatus = rental.getStatus();

        RefundInfo refundInfo = new RefundInfo(false, BigDecimal.ZERO, null);
        if (currentStatus == RentalStatus.CONFIRMED) {
            refundInfo = refundPayment(rentalId);
        }

        rental.cancel(reason);

        Rental updatedRental = rentalRepository.save(rental);
        RentalResponse result = rentalMapper.toDto(updatedRental);

        RentalCancelledEvent event = new RentalCancelledEvent(
                updatedRental.getId(),
                updatedRental.getUserEmail(),
                LocalDateTime.now(clock),
                LocalDateTime.now(clock),
                "Cancelled by " + (currentUser.isAdmin() ? "admin" : "customer") + ". Reason: " + reason,
                refundInfo.refundProcessed(),
                refundInfo.refundAmount(),
                refundInfo.refundTransactionId());
        eventPublisher.publishEvent(event);
        log.info("Published RentalCancelledEvent for rental: {}", updatedRental.getId());

        logRentalOperationSuccess("cancelled", result, "By user: " + username);
        return result;
    }

    private RefundInfo refundPayment(Long rentalId) {
        PaymentDto payment = findPaymentByRentalId(rentalId);

        if (payment.status() == PaymentStatus.CAPTURED) {
            PaymentResult refundResult = paymentService.refundPayment(payment.id(), payment.amount(), cancelRefundKey(rentalId));

            if (!refundResult.success()) {
                throw new PaymentFailedException(
                        payment.transactionId(),
                        "Refund failed: " + refundResult.message());
            }

            return new RefundInfo(true, payment.amount(), refundResult.transactionId());
        } else if (payment.status() == PaymentStatus.AUTHORIZED
                || payment.status() == PaymentStatus.PENDING
                || payment.status() == PaymentStatus.FAILED) {
            PaymentResult cancelResult = paymentService.cancelPayment(payment.id());
            if (!cancelResult.success()) {
                throw new PaymentFailedException(
                        payment.transactionId(),
                        "Payment cancellation failed: " + cancelResult.message());
            }
            boolean holdReleased = payment.status() == PaymentStatus.AUTHORIZED;
            log.info("Payment cancelled before capture. RentalId: {}, HoldReleased: {}", rentalId, holdReleased);

            return holdReleased
                    ? new RefundInfo(true, payment.amount(), payment.transactionId())
                    : new RefundInfo(false, BigDecimal.ZERO, null);
        } else if (payment.status() == PaymentStatus.REFUNDED) {
            return new RefundInfo(true, payment.amount(), payment.transactionId());
        }

        return new RefundInfo(false, BigDecimal.ZERO, null);
    }

    private RefundInfo refundPartialPayment(Rental rental) {
        PaymentDto payment = findPaymentByRentalId(rental.getId());

        if (payment.status() != PaymentStatus.CAPTURED) {
            log.warn("Cannot refund payment in status: {} for rental: {}",
                    payment.status(), rental.getId());
            return new RefundInfo(false, BigDecimal.ZERO, null);
        }

        LocalDate today = LocalDate.now(clock);
        long daysUsed = today.isBefore(rental.getStartDate())
                ? 0
                : ChronoUnit.DAYS.between(rental.getStartDate(), today) + 1;

        if (daysUsed >= rental.getDays()) {
            log.info("No remaining days for refund. RentalId: {}", rental.getId());
            return new RefundInfo(false, BigDecimal.ZERO, null);
        }

        int places = rental.getCurrency().getDecimalPlaces();
        BigDecimal usedPrice = BigDecimal.ZERO;
        if (daysUsed > 0) {
            usedPrice = priceOfDaysUsed(rental, payment.amount(), daysUsed, today, places);
        }
        BigDecimal refundAmount = payment.amount()
                .subtract(usedPrice)
                .max(BigDecimal.ZERO)
                .min(payment.amount())
                .setScale(places, RoundingMode.DOWN);

        log.info("Calculating partial refund: totalDays={}, daysUsed={}, usedPrice={}, amount={}",
                rental.getDays(), daysUsed, usedPrice, refundAmount);

        if (refundAmount.signum() <= 0) {
            return new RefundInfo(false, BigDecimal.ZERO, null);
        }

        PaymentResult refundResult = paymentService.refundPayment(payment.id(), refundAmount, cancelRefundKey(rental.getId()));

        if (!refundResult.success()) {
            throw new PaymentFailedException(
                    payment.transactionId(),
                    "Partial refund failed: " + refundResult.message());
        }

        return new RefundInfo(true, refundAmount, refundResult.transactionId());
    }

    private BigDecimal priceOfDaysUsed(Rental rental, BigDecimal paid, long daysUsed, LocalDate today, int places) {
        BigDecimal prorated = paid.multiply(BigDecimal.valueOf(daysUsed))
                .divide(BigDecimal.valueOf(rental.getDays()), places, RoundingMode.HALF_UP);
        try {
            if (findCarById(rental.getCarId()).currency() != rental.getCurrency()) {
                log.warn("Car currency changed since booking, prorating the early return of rental {}", rental.getId());
                return prorated;
            }
            LocalDate bookingDate = rental.getCreateTime() != null ? rental.getCreateTime().toLocalDate() : today;
            BigDecimal repricedWhole = dynamicPricingService.calculatePrice(
                    rental.getCarId(), rental.getStartDate(), rental.getEndDate(), bookingDate, rental.getId()).finalPrice();
            if (repricedWhole == null || repricedWhole.signum() <= 0) {
                return prorated;
            }
            BigDecimal repricedUsed = dynamicPricingService.calculatePrice(
                    rental.getCarId(), rental.getStartDate(), rental.getStartDate().plusDays(daysUsed - 1),
                    bookingDate, rental.getId()).finalPrice();
            BigDecimal drift = rental.getTotalPrice().divide(repricedWhole, 8, RoundingMode.HALF_UP);
            return repricedUsed.multiply(drift).setScale(places, RoundingMode.HALF_UP);
        } catch (BaseException e) {
            log.warn("Could not reprice the used days of rental {}, prorating: {}", rental.getId(), e.getMessage());
            return prorated;
        }
    }

    private static String cancelRefundKey(Long rentalId) {
        return "rental-" + rentalId + ":cancel-refund";
    }

    private record RefundInfo(boolean refundProcessed, BigDecimal refundAmount, String refundTransactionId) {
    }

    @Override
    public Page<RentalResponse> getMyRentals(String username, Pageable pageable) {
        log.debug("Getting rentals for user: {}", username);

        UserDto user = findUserByUsername(username);
        Page<Rental> rentals = rentalRepository.findByUserIdAndIsDeletedFalse(user.id(), Pageables.stable(pageable));
        Page<RentalResponse> result = rentals.map(rentalMapper::toDto);

        log.info("Successfully retrieved {} rentals for user: {}", result.getTotalElements(), username);
        return result;
    }

    @Override
    public Page<RentalResponse> getAllRentals(Pageable pageable) {
        log.debug("Getting all rentals");

        Page<Rental> rentals = rentalRepository.findByIsDeletedFalse(Pageables.stable(pageable));
        Page<RentalResponse> result = rentals.map(rentalMapper::toDto);

        log.info("Successfully retrieved {} rentals. Page {}/{}",
                result.getNumberOfElements(), result.getNumber() + 1, result.getTotalPages());
        return result;
    }

    @Override
    public RentalResponse getRentalById(Long id, String username) {
        log.debug("Getting rental: {} for user: {}", id, username);

        Rental rental = findRentalById(id);
        UserDto user = findUserByUsername(username);

        if (!user.isAdmin() && !rental.getUserId().equals(user.id())) {
            throw new RentalNotFoundException(id);
        }

        RentalResponse result = rentalMapper.toDto(rental);
        log.info("Successfully retrieved rental: ID={}, Status={}", result.id(), result.status());
        return result;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public PaymentIntentResult createPaymentIntent(Long rentalId, String username) {
        Rental rental = findRentalById(rentalId);
        UserDto user = findUserByUsername(username);

        if (!rental.getUserId().equals(user.id())) {
            throw new RentalNotFoundException(rentalId);
        }
        if (rental.getStatus() != RentalStatus.CONFIRMED) {
            throw new InvalidRentalStateException(rental.getStatus().name(), RentalStatus.CONFIRMED.name());
        }

        LocalDate paymentOpensOn = rental.getStartDate().minusDays(rentalProperties.getPaymentWindowDays());
        if (LocalDate.now(clock).isBefore(paymentOpensOn)) {
            throw new PaymentWindowNotOpenException(paymentOpensOn);
        }

        PaymentIntentResult result = paymentService.createOrGetIntentForRental(rentalId);
        log.info("Payment intent ready for rental: {}, status: {}", rentalId, result.status());
        return result;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public PaymentSummary getRentalPayment(Long rentalId, String username) {
        Rental rental = findRentalById(rentalId);
        UserDto user = findUserByUsername(username);

        if (!user.isAdmin() && !rental.getUserId().equals(user.id())) {
            throw new RentalNotFoundException(rentalId);
        }

        return paymentService.syncAndGetSummaryForRental(rentalId)
                .orElseThrow(() -> new RentalPaymentNotFoundException(rentalId));
    }

    private void requireCarAvailable(Long carId) {
        CarDto car = findCarById(carId);
        if (car.status() != CarStatusType.AVAILABLE) {
            throw new CarNotAvailableException(
                    carId,
                    "Car status is: " + car.status().getDisplayName());
        }
    }

    private Rental findRentalForUpdate(Long id) {
        return rentalRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RentalNotFoundException(id));
    }

    private Rental findRentalById(Long id) {
        return rentalRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RentalNotFoundException(id));
    }

    private CarDto findCarById(Long id) {
        return carCatalog.getCarForBooking(id);
    }

    private UserDto findUserByUsername(String username) {
        return userLookup.getUserByUsername(username);
    }

    private PaymentDto findPaymentByRentalId(Long rentalId) {
        return paymentService.getPaymentByRentalId(rentalId)
                .orElseThrow(() -> new RentalPaymentNotFoundException(rentalId));
    }

    private void logRentalOperationSuccess(String operation, RentalResponse result) {
        log.info("Successfully {} rental: ID={}, Status={}",
                operation, result.id(), result.status());
    }

    private void logRentalOperationSuccess(String operation, RentalResponse result, String extraInfo) {
        log.info("Successfully {} rental: ID={}, Status={}, {}",
                operation, result.id(), result.status(), extraInfo);
    }

    private void processPenaltyForLateReturn(Rental rental, LocalDateTime actualReturnTime) {
        log.debug("Processing penalty for late rental: {}", rental.getId());

        try {
            PenaltyResult penaltyResult = penaltyCalculationService.calculatePenalty(
                    rental, actualReturnTime);

            log.info("Calculated penalty for rental {}: Amount={} {}, Late Hours={}, Late Days={}",
                    rental.getId(),
                    penaltyResult.penaltyAmount(),
                    rental.getCurrency(),
                    penaltyResult.lateHours(),
                    penaltyResult.lateDays());

            rental.flagLate(penaltyResult.status(), actualReturnTime, penaltyResult.lateHours());
            rental.assessPenalty(penaltyResult.penaltyAmount(), penaltyResult.lateHours());

            if (penaltyResult.penaltyAmount() == null ||
                    penaltyResult.penaltyAmount().compareTo(BigDecimal.ZERO) <= 0) {
                log.info("No penalty to charge for rental {} (within grace period or no late hours)",
                        rental.getId());
                rental.recordPenaltyPayment(false);
                return;
            }

            Optional<PaymentDto> alreadyCollected = paymentService.getPenaltyPaymentByRentalId(rental.getId());
            if (alreadyCollected.isPresent()) {
                rental.assessPenalty(alreadyCollected.get().amount(), penaltyResult.lateHours());
                rental.recordPenaltyPayment(true);
                log.warn("Penalty of rental {} was already collected, no second charge is made", rental.getId());
                return;
            }

            PaymentDto penaltyPaymentDto = penaltyPaymentService.createPenaltyPayment(
                    rental, penaltyResult.penaltyAmount());

            PaymentResult chargeResult = penaltyPaymentService.chargePenalty(
                    penaltyPaymentDto.id(), rental.getUserId());

            if (chargeResult.success()) {
                rental.recordPenaltyPayment(true);
                log.info("Successfully charged penalty for rental: {}", rental.getId());
            } else {
                rental.recordPenaltyPayment(false);
                if (!chargeResult.outcomeUnknown()) {
                    penaltyPaymentService.handleFailedPenaltyPayment(penaltyPaymentDto, chargeResult.message());
                }
            }

            publishPenaltySummaryEvent(rental, penaltyResult, actualReturnTime);

        } catch (Exception e) {
            log.error("Error processing penalty for rental {}: {}",
                    rental.getId(), e.getMessage(), e);
            rental.recordPenaltyPayment(false);
        }
    }

    private void publishPenaltySummaryEvent(Rental rental, PenaltyResult penaltyResult,
            LocalDateTime actualReturnTime) {
        LocalDateTime scheduledReturnTime = rental.getEndDate().atTime(23, 59, 59);

        PenaltySummaryEvent event = new PenaltySummaryEvent(
                rental.getId(),
                rental.getUserEmail(),
                LocalDateTime.now(clock),
                rental.getCarBrand(),
                rental.getCarModel(),
                rental.getCarLicensePlate(),
                scheduledReturnTime,
                actualReturnTime,
                penaltyResult.lateHours(),
                penaltyResult.lateDays(),
                penaltyResult.penaltyAmount(),
                rental.getCurrency(),
                penaltyResult.breakdown(),
                penaltyResult.cappedAtMax());

        eventPublisher.publishEvent(event);
        log.info("Published PenaltySummaryEvent for rental: {}", rental.getId());
    }

    @Override
    public RentalSummaryDto getRentalSummaryById(Long rentalId) {
        log.debug("Getting rental summary for cross-module access: {}", rentalId);

        Rental rental = findRentalById(rentalId);

        return new RentalSummaryDto(
                rental.getId(),
                rental.getCarId(),
                rental.getUserId(),
                rental.getCarBrand(),
                rental.getCarModel(),
                rental.getCarLicensePlate(),
                rental.getUserEmail(),
                rental.getUserFullName(),
                rental.getStartDate(),
                rental.getEndDate(),
                Boolean.TRUE.equals(rental.getHasDamageReports()),
                rental.getDamageReportsCount() != null ? rental.getDamageReportsCount() : 0,
                rental.getCurrency());
    }

    @Override
    @Transactional
    public void incrementDamageReportCount(Long rentalId) {
        log.debug("Incrementing damage report count for rental: {}", rentalId);

        Rental rental = findRentalById(rentalId);
        rental.registerDamageReport();

        rentalRepository.save(rental);
        log.info("Incremented damage report count for rental: {} (new count: {})",
                rentalId, rental.getDamageReportsCount());
    }

    @Override
    public int countByStatus(RentalStatus status) {
        return rentalRepository.countByStatusAndIsDeletedFalse(status);
    }

    @Override
    public java.util.Set<Long> findCarIdsInUse() {
        return new java.util.HashSet<>(rentalRepository.findCarIdsInUse());
    }

    @Override
    public java.util.Set<Long> findCarIdsReservedOn(LocalDate date) {
        return new java.util.HashSet<>(rentalRepository.findCarIdsReservedOn(date));
    }

    @Override
    public int countTodaysPickups() {
        return rentalRepository.countTodaysPickups(LocalDate.now(clock));
    }

    @Override
    public int countTodaysReturns() {
        return rentalRepository.countTodaysReturns(LocalDate.now(clock));
    }

    @Override
    public int countOverdueRentals() {
        return rentalRepository.countOverdueRentals(LocalDate.now(clock));
    }

    @Override
    public Page<RentalResponse> findPendingApprovals(Pageable pageable) {
        Page<Rental> rentals = rentalRepository.findPendingApprovals(pageable);
        return rentals.map(rentalMapper::toDto);
    }

    @Override
    public Page<RentalResponse> findTodaysPickups(Pageable pageable) {
        Page<Rental> rentals = rentalRepository.findTodaysPickups(LocalDate.now(clock), pageable);
        return rentals.map(rentalMapper::toDto);
    }

    @Override
    public Page<RentalResponse> findTodaysReturns(Pageable pageable) {
        Page<Rental> rentals = rentalRepository.findTodaysReturns(LocalDate.now(clock), pageable);
        return rentals.map(rentalMapper::toDto);
    }

    @Override
    public Page<RentalResponse> findOverdueRentals(Pageable pageable) {
        Page<Rental> rentals = rentalRepository.findOverdueRentals(LocalDate.now(clock), pageable);
        return rentals.map(rentalMapper::toDto);
    }

    @Override
    public BigDecimal getAverageRentalDurationDays(LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating average rental duration between {} and {}", startDate, endDate);
        Double avgDays = rentalRepository.averageRentalDurationDays(startDate, endDate);
        return BigDecimal.valueOf(avgDays != null ? avgDays : 0.0)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public UserRentalStatisticsDto getUserStatistics(Long userId) {
        log.debug("Calculating rental statistics for user: {}", userId);

        int totalRentals = rentalRepository.countByUserIdAndIsDeletedFalse(userId);
        int completedRentals = rentalRepository.countByUserIdAndStatusAndIsDeletedFalse(userId, RentalStatus.RETURNED);
        int cancelledRentals = rentalRepository.countByUserIdAndStatusAndIsDeletedFalse(userId, RentalStatus.CANCELLED);
        int activeRentals = rentalRepository.countByUserIdAndStatusAndIsDeletedFalse(userId, RentalStatus.IN_USE);

        List<UserRentalStatisticsDto.RentalSpendTotals> spentByCurrency =
                paymentService.sumNetPaidForRentals(rentalRepository.findIdsByUserId(userId)).stream()
                        .map(paid -> new UserRentalStatisticsDto.RentalSpendTotals(
                                paid.currency(), paid.rentalCount(), paid.amount()))
                        .toList();

        Double avgDuration = rentalRepository.averageRentalDurationByUserId(userId);
        double averageRentalDuration = avgDuration != null ? avgDuration : 0.0;

        int lateReturns = rentalRepository.countLateReturnsByUserId(userId);

        log.info(
                "User {} statistics: total={}, completed={}, cancelled={}, active={}, spent={}, avgDuration={}, lateReturns={}",
                userId, totalRentals, completedRentals, cancelledRentals, activeRentals, spentByCurrency,
                averageRentalDuration, lateReturns);

        return new UserRentalStatisticsDto(
                totalRentals,
                completedRentals,
                cancelledRentals,
                activeRentals,
                spentByCurrency,
                averageRentalDuration,
                lateReturns);
    }

    @Override
    public Page<RentalResponse> getUserRentals(Long userId, RentalStatus status, Pageable pageable) {
        log.debug("Getting rentals for user: {}, status filter: {}", userId, status);

        Page<Rental> rentals;
        if (status != null) {
            rentals = rentalRepository.findByUserIdAndStatusAndIsDeletedFalse(userId, status, Pageables.stable(pageable));
        } else {
            rentals = rentalRepository.findByUserIdAndIsDeletedFalse(userId, Pageables.stable(pageable));
        }

        Page<RentalResponse> result = rentals.map(rentalMapper::toDto);
        log.info("Retrieved {} rentals for user: {}", result.getTotalElements(), userId);
        return result;
    }

    @Override
    public Page<RentalResponse> getCarRentals(Long carId, RentalStatus status, Pageable pageable) {
        log.debug("Getting rentals for car: {}, status filter: {}", carId, status);

        Page<Rental> rentals;
        if (status != null) {
            rentals = rentalRepository.findByCarIdAndStatusAndIsDeletedFalse(carId, status, Pageables.stable(pageable));
        } else {
            rentals = rentalRepository.findByCarIdAndIsDeletedFalse(carId, Pageables.stable(pageable));
        }

        Page<RentalResponse> result = rentals.map(rentalMapper::toDto);
        log.info("Retrieved {} rentals for car: {}", result.getTotalElements(), carId);
        return result;
    }
}