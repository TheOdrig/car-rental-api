package com.akif.service.impl;

import com.akif.dto.request.RentalRequestDto;
import com.akif.dto.response.RentalResponseDto;
import com.akif.enums.*;
import com.akif.exception.*;
import com.akif.mapper.RentalMapper;
import com.akif.model.Car;
import com.akif.model.Payment;
import com.akif.model.Rental;
import com.akif.model.User;
import com.akif.repository.CarRepository;
import com.akif.repository.PaymentRepository;
import com.akif.repository.RentalRepository;
import com.akif.repository.UserRepository;
import com.akif.service.gateway.IPaymentGateway;
import com.akif.service.IRentalService;
import com.akif.service.gateway.PaymentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RentalServiceImpl implements IRentalService {

    private final RentalRepository rentalRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final IPaymentGateway paymentGateway;
    private final RentalMapper rentalMapper;

    @Override
    @Transactional
    public RentalResponseDto requestRental(RentalRequestDto request, String username) {
        log.info("Creating rental request for user: {}, car: {}", username, request.getCarId());

        User user = findUserByUsername(username);

        Car car = findCarById(request.getCarId());
        validateCarAvailability(car);

        validateRentalDates(request.getStartDate(), request.getEndDate());

        checkDateOverlap(car.getId(), request.getStartDate(), request.getEndDate());

        int days = calculateDays(request.getStartDate(), request.getEndDate());

        BigDecimal dailyPrice = car.getPrice();
        BigDecimal totalPrice = dailyPrice.multiply(BigDecimal.valueOf(days));
        CurrencyType currency = car.getCurrencyType();

        Rental rental = Rental.builder()
                .user(user)
                .car(car)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .days(days)
                .dailyPrice(dailyPrice)
                .totalPrice(totalPrice)
                .currency(currency)
                .status(RentalStatus.REQUESTED)
                .build();

        Rental savedRental = rentalRepository.save(rental);
        RentalResponseDto result = rentalMapper.toDto(savedRental);

        logRentalOperationSuccess("created", result);
        return result;
    }

    private void validateCarAvailability(Car car) {
        if (!car.getCarStatusType().equals(CarStatusType.AVAILABLE)) {
            throw new CarNotAvailableException(
                    car.getId(),
                    "Car status is: " + car.getCarStatusType().getDisplayName()
            );
        }
        if (car.isDeleted()) {
            throw new CarNotAvailableException(car.getId(), "Car is deleted");
        }
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
    public RentalResponseDto confirmRental(Long rentalId) {
        log.info("Confirming rental: {}", rentalId);

        Rental rental = findRentalById(rentalId);

        if (!rental.getStatus().canConfirm()) {
            throw new InvalidRentalStateException(
                    rental.getStatus().name(),
                    RentalStatus.REQUESTED.name()
            );
        }

        PaymentResult authResult = paymentGateway.authorize(
                rental.getTotalPrice(),
                rental.getCurrency(),
                rental.getUser().getId().toString()
        );

        if (!authResult.success()) {
            throw new PaymentFailedException(
                    "Payment authorization failed: " + authResult.message()
            );
        }

        Payment payment = Payment.builder()
                .rental(rental)
                .amount(rental.getTotalPrice())
                .currency(rental.getCurrency())
                .status(PaymentStatus.AUTHORIZED)
                .transactionId(authResult.transactionId())
                .paymentMethod("DEFAULT")
                .gatewayResponse(authResult.message())
                .build();

        paymentRepository.save(payment);

        rental.updateStatus(RentalStatus.CONFIRMED);

        rental.getCar().setCarStatusType(CarStatusType.RESERVED);

        Rental updatedRental = rentalRepository.save(rental);
        RentalResponseDto result = rentalMapper.toDto(updatedRental);

        logRentalOperationSuccess("confirmed", result, "TransactionId: " + authResult.transactionId());
        return result;
    }

    @Override
    @Transactional
    public RentalResponseDto pickupRental(Long rentalId, String pickupNotes) {
        log.info("Processing pickup for rental: {}", rentalId);

        Rental rental = findRentalById(rentalId);

        if (!rental.getStatus().canPickup()) {
            throw new InvalidRentalStateException(
                    rental.getStatus().name(),
                    RentalStatus.CONFIRMED.name()
            );
        }

        Payment payment = findPaymentByRentalId(rentalId);
        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new InvalidRentalStateException(
                    "Payment must be AUTHORIZED before pickup. Current status: " + payment.getStatus()
            );
        }

        PaymentResult captureResult = paymentGateway.capture(
                payment.getTransactionId(),
                payment.getAmount()
        );

        if (!captureResult.success()) {
            throw new PaymentFailedException(
                    payment.getTransactionId(),
                    "Payment capture failed: " + captureResult.message()
            );
        }

        payment.updateStatus(PaymentStatus.CAPTURED);
        paymentRepository.save(payment);

        rental.updateStatus(RentalStatus.IN_USE);
        rental.setPickupNotes(pickupNotes);
        Rental updatedRental = rentalRepository.save(rental);
        RentalResponseDto result = rentalMapper.toDto(updatedRental);

        logRentalOperationSuccess("picked up", result, "Notes: " + (pickupNotes != null ? pickupNotes : "None"));
        return result;
    }

    @Override
    @Transactional
    public RentalResponseDto returnRental(Long rentalId, String returnNotes) {
        log.info("Processing return for rental: {}", rentalId);

        Rental rental = findRentalById(rentalId);

        if (!rental.getStatus().canReturn()) {
            throw new InvalidRentalStateException(
                    rental.getStatus().name(),
                    RentalStatus.IN_USE.name()
            );
        }

        rental.updateStatus(RentalStatus.RETURNED);
        rental.setReturnNotes(returnNotes);

        Car car = rental.getCar();
        car.setCarStatusType(CarStatusType.AVAILABLE);
        Rental updatedRental = rentalRepository.save(rental);
        RentalResponseDto result = rentalMapper.toDto(updatedRental);

        logRentalOperationSuccess("returned", result, "Notes: " + (returnNotes != null ? returnNotes : "None"));
        return result;
    }

    @Override
    @Transactional
    public RentalResponseDto cancelRental(Long rentalId, String username) {
        log.info("Cancelling rental: {} by user: {}", rentalId, username);

        Rental rental = findRentalById(rentalId);
        User currentUser = findUserByUsername(username);

        if (!currentUser.getRoles().contains(Role.ADMIN) &&
                !rental.getUser().getId().equals(currentUser.getId())) {
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

        if (currentStatus == RentalStatus.CONFIRMED) {
            refundPayment(rentalId);
        }

        rental.updateStatus(RentalStatus.CANCELLED);

        Car car = rental.getCar();
        if (car.getCarStatusType() == CarStatusType.RESERVED) {
            car.setCarStatusType(CarStatusType.AVAILABLE);
        }

        Rental updatedRental = rentalRepository.save(rental);
        RentalResponseDto result = rentalMapper.toDto(updatedRental);

        logRentalOperationSuccess("cancelled", result, "By user: " + username);
        return result;
    }

    private void refundPayment(Long rentalId) {
        Payment payment = findPaymentByRentalId(rentalId);

        if (payment.getStatus() == PaymentStatus.CAPTURED) {

            PaymentResult refundResult = paymentGateway.refund(
                    payment.getTransactionId(),
                    payment.getAmount()
            );

            if (!refundResult.success()) {
                throw new PaymentFailedException(
                        payment.getTransactionId(),
                        "Refund failed: " + refundResult.message()
                );
            }

            payment.updateStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        } else if (payment.getStatus() == PaymentStatus.AUTHORIZED) {

            payment.updateStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
            log.info("Payment was only authorized, no refund needed. RentalId: {}", rentalId);
        }
    }

    @Override
    public Page<RentalResponseDto> getMyRentals(String username, Pageable pageable) {
        log.debug("Getting rentals for user: {}", username);

        User user = findUserByUsername(username);
        Page<Rental> rentals = rentalRepository.findByUserIdAndIsDeletedFalse(user.getId(), pageable);
        Page<RentalResponseDto> result = rentals.map(rentalMapper::toDto);

        log.info("Successfully retrieved {} rentals for user: {}", result.getTotalElements(), username);
        return result;
    }

    @Override
    public Page<RentalResponseDto> getAllRentals(Pageable pageable) {
        log.debug("Getting all rentals");
        
        Page<Rental> rentals = rentalRepository.findByIsDeletedFalse(pageable);
        Page<RentalResponseDto> result = rentals.map(rentalMapper::toDto);

        log.info("Successfully retrieved {} rentals. Page {}/{}", 
                result.getNumberOfElements(), result.getNumber() + 1, result.getTotalPages());
        return result;
    }

    @Override
    public RentalResponseDto getRentalById(Long id, String username) {
        log.debug("Getting rental: {} for user: {}", id, username);

        Rental rental = findRentalById(id);
        User user = findUserByUsername(username);

        if (!user.getRoles().contains(com.akif.enums.Role.ADMIN) &&
                !rental.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    "You can only view your own rentals"
            );
        }

        RentalResponseDto result = rentalMapper.toDto(rental);
        log.info("Successfully retrieved rental: ID={}, Status={}", result.getId(), result.getStatus());
        return result;
    }


    private Rental findRentalById(Long id) {
        return rentalRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RentalNotFoundException(id));
    }

    private Car findCarById(Long id) {
        return carRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new CarNotFoundException(id));
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private Payment findPaymentByRentalId(Long rentalId) {
        return paymentRepository.findByRentalIdAndIsDeletedFalse(rentalId)
                .orElseThrow(() -> new RentalNotFoundException(
                        "Payment not found for rental: " + rentalId
                ));
    }


    private void logRentalOperationSuccess(String operation, RentalResponseDto result) {
        log.info("Successfully {} rental: ID={}, Status={}", 
                operation, result.getId(), result.getStatus());
    }

    private void logRentalOperationSuccess(String operation, RentalResponseDto result, String extraInfo) {
        log.info("Successfully {} rental: ID={}, Status={}, {}", 
                operation, result.getId(), result.getStatus(), extraInfo);
    }
}