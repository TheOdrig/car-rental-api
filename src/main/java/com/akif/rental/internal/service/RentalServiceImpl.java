package com.akif.rental.internal.service;

import com.akif.car.api.CarResponse;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.car.internal.exception.CarNotAvailableException;
import com.akif.payment.internal.exception.PaymentFailedException;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.api.RentalSummaryDto;
import com.akif.rental.domain.enums.LateReturnStatus;
import com.akif.payment.api.PaymentDto;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.api.CreatePaymentRequest;
import com.akif.rental.domain.enums.RentalStatus;
import com.akif.rental.internal.exception.InvalidRentalStateException;
import com.akif.rental.internal.exception.RentalDateOverlapException;
import com.akif.rental.internal.exception.RentalNotFoundException;
import com.akif.rental.internal.exception.RentalValidationException;
import com.akif.rental.internal.dto.penalty.PenaltyResult;
import com.akif.payment.api.PaymentCapturedEvent;
import com.akif.rental.api.PenaltySummaryEvent;
import com.akif.rental.api.RentalCancelledEvent;
import com.akif.rental.api.RentalConfirmedEvent;
import com.akif.rental.internal.dto.request.RentalRequest;
import com.akif.rental.internal.mapper.RentalMapper;
import com.akif.rental.domain.model.Rental;
import com.akif.car.api.CarService;
import com.akif.auth.api.AuthService;
import com.akif.auth.api.UserDto;
import com.akif.rental.internal.service.penalty.PenaltyCalculationService;
import com.akif.rental.internal.service.penalty.PenaltyPaymentService;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.payment.api.PaymentService;
import com.akif.rental.api.RentalService;
import com.akif.payment.api.PaymentResult;
import com.akif.car.internal.service.pricing.DynamicPricingService;
import com.akif.car.internal.dto.pricing.PriceModifier;
import com.akif.car.internal.dto.pricing.PricingResult;
import com.akif.shared.enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RentalServiceImpl implements RentalService {

    private static final String STUB_PAYMENT_METHOD = "STUB_GATEWAY";
    
    private final RentalRepository rentalRepository;

    private final CarService carService;
    private final AuthService authService;

    private final PaymentService paymentService;
    private final RentalMapper rentalMapper;
    private final DynamicPricingService dynamicPricingService;
    private final ApplicationEventPublisher eventPublisher;
    private final PenaltyCalculationService penaltyCalculationService;
    private final PenaltyPaymentService penaltyPaymentService;

    @Override
    @Transactional
    public RentalResponse requestRental(RentalRequest request, String username) {
        log.info("Creating rental request for user: {}, car: {}", username, request.carId());
        
        UserDto user = findUserByUsername(username);
        CarResponse car = findCarById(request.carId());

        if (!car.getCarStatusType().equals(CarStatusType.AVAILABLE)) {
            throw new CarNotAvailableException(
                    car.getId(),
                    "Car status is: " + car.getCarStatusType().getDisplayName()
            );
        }

        validateRentalDates(request.startDate(), request.endDate());
        checkDateOverlap(car.getId(), request.startDate(), request.endDate());

        PricingResult pricingResult = dynamicPricingService.calculatePrice(
            request.carId(),
            request.startDate(),
            request.endDate(),
            LocalDate.now()
        );

        int days = pricingResult.rentalDays();
        BigDecimal dailyPrice = pricingResult.effectiveDailyPrice();
        BigDecimal totalPrice = pricingResult.finalPrice();
        CurrencyType currency = car.getCurrencyType();

        log.info("Dynamic pricing applied: base={}, final={}, modifiers={}",
            pricingResult.baseTotalPrice(), pricingResult.finalPrice(), pricingResult.appliedModifiers().size());
        
        Rental rental = Rental.builder()
                .userId(user.id())
                .carId(car.getId())
                .carBrand(car.getBrand())
                .carModel(car.getModel())
                .carLicensePlate(car.getLicensePlate())
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

        Rental savedRental = rentalRepository.save(rental);
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
            result.createTime(),
            result.updateTime()
        );

        logRentalOperationSuccess("created", result);
        return result;
    }

    private void validateRentalDates(LocalDate startDate, LocalDate endDate) {
        if (startDate.isBefore(LocalDate.now())) {
            throw new RentalValidationException("Start date cannot be in the past");
        }
        if (endDate.isBefore(startDate)) {
            throw new RentalValidationException("End date must be after start date");
        }
        if (startDate.isAfter(endDate)) {
            throw new RentalValidationException("Invalid date range");
        }
    }

    private void checkDateOverlap(Long carId, LocalDate startDate, LocalDate endDate) {
        long overlappingCount = rentalRepository.countOverlappingRentals(carId, startDate, endDate);
        if (overlappingCount > 0) {
            throw new RentalDateOverlapException(carId, startDate, endDate);
        }
    }

    private int calculateDays(LocalDate startDate, LocalDate endDate) {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    @Override
    @Transactional
    public RentalResponse confirmRental(Long rentalId) {
        log.info("Confirming rental: {}", rentalId);

        Rental rental = findRentalById(rentalId);

        if (!rental.getStatus().canConfirm()) {
            throw new InvalidRentalStateException(
                    rental.getStatus().name(),
                    RentalStatus.REQUESTED.name()
            );
        }

        checkDateOverlap(rental.getCarId(), rental.getStartDate(), rental.getEndDate());

        PaymentResult authResult = paymentService.authorize(
                rental.getTotalPrice(),
                rental.getCurrency(),
                rental.getUserId().toString()
        );

        if (!authResult.success()) {
            throw new PaymentFailedException(
                    "Payment authorization failed: " + authResult.message()
            );
        }

        PaymentDto paymentDto = paymentService.createPayment(
                new CreatePaymentRequest(
                        rental.getId(),
                        rental.getUserEmail(),
                        rental.getCarLicensePlate(),
                        rental.getTotalPrice(),
                        rental.getCurrency(),
                        STUB_PAYMENT_METHOD
                )
        );

        paymentService.updatePaymentStatus(
                paymentDto.id(), 
                PaymentStatus.AUTHORIZED, 
                authResult.transactionId(), 
                null
        );

        rental.updateStatus(RentalStatus.CONFIRMED);

        carService.reserveCar(rental.getCarId());

        Rental updatedRental = rentalRepository.save(rental);
        RentalResponse result = rentalMapper.toDto(updatedRental);

        RentalConfirmedEvent event = new RentalConfirmedEvent(
                this,
                updatedRental.getId(),
                updatedRental.getUserEmail(),
                LocalDateTime.now(),
                updatedRental.getCarBrand(),
                updatedRental.getCarModel(),
                updatedRental.getStartDate(),
                updatedRental.getEndDate(),
                updatedRental.getTotalPrice(),
                updatedRental.getCurrency(),
                "Main Office"
        );
        eventPublisher.publishEvent(event);
        log.info("Published RentalConfirmedEvent for rental: {}", updatedRental.getId());

        logRentalOperationSuccess("confirmed", result, "TransactionId: " + authResult.transactionId());
        return result;
    }

    @Override
    @Transactional
    public RentalResponse pickupRental(Long rentalId, String pickupNotes) {
        log.info("Processing pickup for rental: {}", rentalId);

        Rental rental = findRentalById(rentalId);

        if (!rental.getStatus().canPickup()) {
            throw new InvalidRentalStateException(
                    rental.getStatus().name(),
                    RentalStatus.CONFIRMED.name()
            );
        }

        PaymentDto payment = findPaymentByRentalId(rentalId);
        if (payment.status() != PaymentStatus.AUTHORIZED) {
            throw new InvalidRentalStateException(
                    "Payment must be AUTHORIZED before pickup. Current status: " + payment.status()
            );
        }

        PaymentResult captureResult = paymentService.capture(
                payment.transactionId(),
                payment.amount()
        );

        if (!captureResult.success()) {
            throw new PaymentFailedException(
                    payment.transactionId(),
                    "Payment capture failed: " + captureResult.message()
            );
        }

        PaymentDto savedPayment = paymentService.updatePaymentStatus(
                payment.id(),
                PaymentStatus.CAPTURED,
                captureResult.transactionId(),
                null
        );

        rental.updateStatus(RentalStatus.IN_USE);
        rental.setPickupNotes(pickupNotes);
        Rental updatedRental = rentalRepository.save(rental);
        RentalResponse result = rentalMapper.toDto(updatedRental);

        PaymentCapturedEvent event = new PaymentCapturedEvent(
                this,
                savedPayment.id(),
                updatedRental.getId(),
                updatedRental.getUserEmail(),
                savedPayment.amount(),
                savedPayment.currency(),
                savedPayment.transactionId(),
                LocalDateTime.now()
        );
        eventPublisher.publishEvent(event);
        log.info("Published PaymentCapturedEvent for payment: {}, rental: {}", savedPayment.id(), updatedRental.getId());

        logRentalOperationSuccess("picked up", result, "Notes: " + (pickupNotes != null ? pickupNotes : "None"));
        return result;
    }

    @Override
    @Transactional
    public RentalResponse returnRental(Long rentalId, String returnNotes) {
        log.info("Processing return for rental: {}", rentalId);

        Rental rental = findRentalById(rentalId);

        if (!rental.getStatus().canReturn()) {
            throw new InvalidRentalStateException(
                    rental.getStatus().name(),
                    RentalStatus.IN_USE.name()
            );
        }

        LocalDateTime actualReturnTime = LocalDateTime.now();
        rental.setActualReturnTime(actualReturnTime);

        if (rental.getLateReturnStatus() != null && 
            rental.getLateReturnStatus() != LateReturnStatus.ON_TIME) {
            
            log.info("Processing late return penalty for rental: {}", rentalId);
            processPenaltyForLateReturn(rental, actualReturnTime);
        }

        rental.updateStatus(RentalStatus.RETURNED);
        rental.setReturnNotes(returnNotes);

        carService.releaseCar(rental.getCarId());
        
        Rental updatedRental = rentalRepository.save(rental);
        RentalResponse result = rentalMapper.toDto(updatedRental);

        logRentalOperationSuccess("returned", result, "Notes: " + (returnNotes != null ? returnNotes : "None"));
        return result;
    }

    @Override
    @Transactional
    public RentalResponse cancelRental(Long rentalId, String username) {
        log.info("Cancelling rental: {} by user: {}", rentalId, username);

        Rental rental = findRentalById(rentalId);
        UserDto currentUser = findUserByUsername(username);

        if (!currentUser.isAdmin() && !rental.getUserId().equals(currentUser.id())) {
            throw new AccessDeniedException(
                    "You can only cancel your own rentals"
            );
        }

        if (!rental.getStatus().canCancel()) {
            throw new InvalidRentalStateException(
                    "Cannot cancel rental in status: " + rental.getStatus().name()
            );
        }

        RentalStatus currentStatus = rental.getStatus();

        RefundInfo refundInfo = new RefundInfo(false, BigDecimal.ZERO, null);
        if (currentStatus == RentalStatus.CONFIRMED) {
            refundInfo = refundPayment(rentalId);
        } else if (currentStatus == RentalStatus.IN_USE) {
            refundInfo = refundPartialPayment(rental);
        }

        rental.updateStatus(RentalStatus.CANCELLED);

        CarResponse car = carService.getCarById(rental.getCarId());
        if (car.getCarStatusType() == CarStatusType.RESERVED) {
            carService.releaseCar(rental.getCarId());
        }

        Rental updatedRental = rentalRepository.save(rental);
        RentalResponse result = rentalMapper.toDto(updatedRental);

        RentalCancelledEvent event = new RentalCancelledEvent(
                this,
                updatedRental.getId(),
                updatedRental.getUserEmail(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                "Cancelled by " + (currentUser.isAdmin() ? "admin" : "customer"),
                refundInfo.refundProcessed(),
                refundInfo.refundAmount(),
                refundInfo.refundTransactionId()
        );
        eventPublisher.publishEvent(event);
        log.info("Published RentalCancelledEvent for rental: {}", updatedRental.getId());

        logRentalOperationSuccess("cancelled", result, "By user: " + username);
        return result;
    }

    private RefundInfo refundPayment(Long rentalId) {
        PaymentDto payment = findPaymentByRentalId(rentalId);

        if (payment.status() == PaymentStatus.CAPTURED) {
            PaymentResult refundResult = paymentService.refundPayment(payment.id(), payment.amount());

            if (!refundResult.success()) {
                throw new PaymentFailedException(
                        payment.transactionId(),
                        "Refund failed: " + refundResult.message()
                );
            }

            return new RefundInfo(true, payment.amount(), refundResult.transactionId());
        } else if (payment.status() == PaymentStatus.AUTHORIZED) {
            paymentService.updatePaymentStatus(payment.id(), PaymentStatus.REFUNDED, null, null);
            log.info("Payment was only authorized, no refund needed. RentalId: {}", rentalId);
            
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

        LocalDate today = LocalDate.now();
        LocalDate endDate = rental.getEndDate();
        long remainingDays = ChronoUnit.DAYS.between(today, endDate);
        
        if (remainingDays <= 0) {
            log.info("No remaining days for refund. RentalId: {}", rental.getId());
            return new RefundInfo(false, BigDecimal.ZERO, null);
        }

        long totalDays = rental.getDays();
        BigDecimal refundPercentage = BigDecimal.valueOf(remainingDays)
                .divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP);
        BigDecimal refundAmount = payment.amount()
                .multiply(refundPercentage)
                .setScale(2, RoundingMode.HALF_UP);

        log.info("Calculating partial refund: totalDays={}, remainingDays={}, percentage={}, amount={}", 
                totalDays, remainingDays, refundPercentage, refundAmount);

        PaymentResult refundResult = paymentService.refundPayment(payment.id(), refundAmount);

        if (!refundResult.success()) {
            throw new PaymentFailedException(
                    payment.transactionId(),
                    "Partial refund failed: " + refundResult.message()
            );
        }

        return new RefundInfo(true, refundAmount, refundResult.transactionId());
    }
    
    private record RefundInfo(boolean refundProcessed, BigDecimal refundAmount, String refundTransactionId) {}

    @Override
    public Page<RentalResponse> getMyRentals(String username, Pageable pageable) {
        log.debug("Getting rentals for user: {}", username);

        UserDto user = findUserByUsername(username);
        Page<Rental> rentals = rentalRepository.findByUserIdAndIsDeletedFalse(user.id(), pageable);
        Page<RentalResponse> result = rentals.map(rentalMapper::toDto);

        log.info("Successfully retrieved {} rentals for user: {}", result.getTotalElements(), username);
        return result;
    }

    @Override
    public Page<RentalResponse> getAllRentals(Pageable pageable) {
        log.debug("Getting all rentals");
        
        Page<Rental> rentals = rentalRepository.findByIsDeletedFalse(pageable);
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
            throw new AccessDeniedException(
                    "You can only view your own rentals"
            );
        }

        RentalResponse result = rentalMapper.toDto(rental);
        log.info("Successfully retrieved rental: ID={}, Status={}", result.id(), result.status());
        return result;
    }


    private Rental findRentalById(Long id) {
        return rentalRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RentalNotFoundException(id));
    }

    
    private CarResponse findCarById(Long id) {
        return carService.getCarById(id);
    }

    private UserDto findUserByUsername(String username) {
        return authService.getUserByUsername(username);
    }

    private PaymentDto findPaymentByRentalId(Long rentalId) {
        return paymentService.getPaymentByRentalId(rentalId)
                .orElseThrow(() -> new RentalNotFoundException(
                        "Payment not found for rental: " + rentalId
                ));
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

            rental.setPenaltyAmount(penaltyResult.penaltyAmount());
            rental.setLateHours(penaltyResult.lateHours());

            PaymentDto penaltyPaymentDto = penaltyPaymentService.createPenaltyPayment(
                    rental, penaltyResult.penaltyAmount());

            PaymentResult chargeResult = penaltyPaymentService.chargePenalty(
                    penaltyPaymentDto.id(), rental.getUserId());

            if (chargeResult.success()) {
                rental.setPenaltyPaid(true);
                log.info("Successfully charged penalty for rental: {}", rental.getId());
            } else {
                rental.setPenaltyPaid(false);
                // Use new interface signature (paymentId, rentalId, userEmail, failureReason)
                penaltyPaymentService.handleFailedPenaltyPayment(
                        penaltyPaymentDto.id(),
                        rental.getId(),
                        rental.getUserEmail(),
                        chargeResult.message()
                );
                log.warn("Failed to charge penalty for rental: {}, Reason: {}", 
                        rental.getId(), chargeResult.message());
            }

            publishPenaltySummaryEvent(rental, penaltyResult, actualReturnTime);

        } catch (Exception e) {
            log.error("Error processing penalty for rental {}: {}", 
                     rental.getId(), e.getMessage(), e);
            rental.setPenaltyPaid(false);
        }
    }

    private void publishPenaltySummaryEvent(Rental rental, PenaltyResult penaltyResult, 
                                           LocalDateTime actualReturnTime) {
        LocalDateTime scheduledReturnTime = rental.getEndDate().atTime(23, 59, 59);
        
        PenaltySummaryEvent event = new PenaltySummaryEvent(
                this,
                rental.getId(),
                rental.getUserEmail(),
                LocalDateTime.now(),
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
                penaltyResult.cappedAtMax()
        );

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
                rental.getDamageReportsCount() != null ? rental.getDamageReportsCount() : 0
        );
    }

    @Override
    @Transactional
    public void incrementDamageReportCount(Long rentalId) {
        log.debug("Incrementing damage report count for rental: {}", rentalId);
        
        Rental rental = findRentalById(rentalId);
        rental.setHasDamageReports(true);
        
        Integer currentCount = rental.getDamageReportsCount();
        rental.setDamageReportsCount((currentCount != null ? currentCount : 0) + 1);
        
        rentalRepository.save(rental);
        log.info("Incremented damage report count for rental: {} (new count: {})", 
                rentalId, rental.getDamageReportsCount());
    }
}