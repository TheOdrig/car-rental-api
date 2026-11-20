package com.akif.rental.internal.service;

import com.akif.shared.web.Pageables;
import com.akif.shared.security.OwnerOrAdmin;
import java.time.Clock;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.api.RentalSummaryDto;
import com.akif.payment.api.PaymentIntentResult;
import com.akif.payment.api.PaymentSummary;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.internal.exception.InvalidRentalStateException;
import com.akif.rental.internal.exception.RentalPaymentNotFoundException;
import com.akif.rental.internal.exception.RentalNotFoundException;

import com.akif.rental.domain.model.Rental;
import com.akif.auth.api.UserDto;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.payment.api.PaymentService;
import com.akif.auth.api.UserRentalStatisticsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RentalQueryService {

    private final Clock clock;
    private final RentalRepository rentalRepository;
    private final RentalAssembler assembler;
    private final PaymentService paymentService;
    private final PaymentWindow paymentWindow;
    private final RentalLookups lookups;

    public Page<RentalResponse> getMyRentals(Long userId, Pageable pageable) {
        log.debug("Getting rentals for user: {}", userId);

        UserDto user = lookups.findUserById(userId);
        Page<Rental> rentals = rentalRepository.findByUserIdAndIsDeletedFalse(user.id(), Pageables.stable(pageable));
        Page<RentalResponse> result = assembler.toPage(rentals);

        log.info("Successfully retrieved {} rentals for user: {}", result.getTotalElements(), userId);
        return result;
    }

    public Page<RentalResponse> getAllRentals(java.util.Collection<RentalStatus> statuses, Pageable pageable) {
        log.debug("Getting all rentals, status filter: {}", statuses);

        Page<Rental> rentals = statuses == null || statuses.isEmpty()
                ? rentalRepository.findByIsDeletedFalse(Pageables.stable(pageable))
                : rentalRepository.findByStatusInAndIsDeletedFalse(statuses, Pageables.stable(pageable));
        Page<RentalResponse> result = assembler.toPage(rentals);

        log.info("Successfully retrieved {} rentals. Page {}/{}",
                result.getNumberOfElements(), result.getNumber() + 1, result.getTotalPages());
        return result;
    }

    public RentalResponse getRentalById(Long id, Long userId) {
        log.debug("Getting rental: {} for user: {}", id, userId);

        Rental rental = lookups.findRentalById(id);
        UserDto user = lookups.findUserById(userId);

        if (!OwnerOrAdmin.permits(user.isAdmin(), rental.getUserId(), user.id())) {
            throw new RentalNotFoundException(id);
        }

        RentalResponse result = assembler.toResponse(rental);
        log.info("Successfully retrieved rental: ID={}, Status={}", result.id(), result.status());
        return result;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public PaymentIntentResult createPaymentIntent(Long rentalId, Long userId) {
        Rental rental = lookups.findRentalById(rentalId);
        UserDto user = lookups.findUserById(userId);

        if (!rental.getUserId().equals(user.id())) {
            throw new RentalNotFoundException(rentalId);
        }
        if (rental.getStatus() != RentalStatus.CONFIRMED) {
            throw new InvalidRentalStateException(rental.getStatus().name(), RentalStatus.CONFIRMED.name());
        }

        paymentWindow.requireOpen(rental.getStartDate());
        paymentWindow.requireNotOverdue(rental.getStartDate(), rental.getConfirmedAt());

        PaymentIntentResult result = paymentService.createOrGetIntentForRental(rentalId);
        log.info("Payment intent ready for rental: {}, status: {}", rentalId, result.status());
        return result;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public PaymentSummary getRentalPayment(Long rentalId, Long userId) {
        Rental rental = lookups.findRentalById(rentalId);
        UserDto user = lookups.findUserById(userId);

        if (!OwnerOrAdmin.permits(user.isAdmin(), rental.getUserId(), user.id())) {
            throw new RentalNotFoundException(rentalId);
        }

        return paymentService.syncAndGetSummaryForRental(rentalId)
                .orElseThrow(() -> new RentalPaymentNotFoundException(rentalId));
    }

    public RentalSummaryDto getRentalSummaryById(Long rentalId) {
        log.debug("Getting rental summary for cross-module access: {}", rentalId);

        Rental rental = lookups.findRentalById(rentalId);

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
                rental.getCurrency(),
                rental.getStatus(),
                rental.getActualReturnTime());
    }


    public int countByStatus(RentalStatus status) {
        return rentalRepository.countByStatusAndIsDeletedFalse(status);
    }

    public java.util.Set<Long> findCarIdsInUse() {
        return new java.util.HashSet<>(rentalRepository.findCarIdsInUse());
    }

    public java.util.Set<Long> findCarIdsReservedOn(LocalDate date) {
        return new java.util.HashSet<>(rentalRepository.findCarIdsReservedOn(date));
    }

    public int countTodaysPickups() {
        return rentalRepository.countTodaysPickups(LocalDate.now(clock));
    }

    public int countTodaysReturns() {
        return rentalRepository.countTodaysReturns(LocalDate.now(clock));
    }

    public int countOverdueRentals() {
        return rentalRepository.countOverdueRentals(LocalDate.now(clock));
    }

    public Page<RentalResponse> findPendingApprovals(Pageable pageable) {
        Page<Rental> rentals = rentalRepository.findPendingApprovals(pageable);
        return assembler.toPage(rentals);
    }

    public Page<RentalResponse> findTodaysPickups(Pageable pageable) {
        Page<Rental> rentals = rentalRepository.findTodaysPickups(LocalDate.now(clock), pageable);
        return assembler.toPage(rentals);
    }

    public Page<RentalResponse> findTodaysReturns(Pageable pageable) {
        Page<Rental> rentals = rentalRepository.findTodaysReturns(LocalDate.now(clock), pageable);
        return assembler.toPage(rentals);
    }

    public Page<RentalResponse> findOverdueRentals(Pageable pageable) {
        Page<Rental> rentals = rentalRepository.findOverdueRentals(LocalDate.now(clock), pageable);
        return assembler.toPage(rentals);
    }

    public BigDecimal getAverageRentalDurationDays(LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating average rental duration between {} and {}", startDate, endDate);
        Double avgDays = rentalRepository.averageRentalDurationDays(startDate, endDate);
        return BigDecimal.valueOf(avgDays != null ? avgDays : 0.0)
                .setScale(2, RoundingMode.HALF_UP);
    }

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

    public Page<RentalResponse> getUserRentals(Long userId, RentalStatus status, Pageable pageable) {
        log.debug("Getting rentals for user: {}, status filter: {}", userId, status);

        Page<Rental> rentals;
        if (status != null) {
            rentals = rentalRepository.findByUserIdAndStatusAndIsDeletedFalse(userId, status, Pageables.stable(pageable));
        } else {
            rentals = rentalRepository.findByUserIdAndIsDeletedFalse(userId, Pageables.stable(pageable));
        }

        Page<RentalResponse> result = assembler.toPage(rentals);
        log.info("Retrieved {} rentals for user: {}", result.getTotalElements(), userId);
        return result;
    }

    public Page<RentalResponse> getCarRentals(Long carId, RentalStatus status, Pageable pageable) {
        log.debug("Getting rentals for car: {}, status filter: {}", carId, status);

        Page<Rental> rentals;
        if (status != null) {
            rentals = rentalRepository.findByCarIdAndStatusAndIsDeletedFalse(carId, status, Pageables.stable(pageable));
        } else {
            rentals = rentalRepository.findByCarIdAndIsDeletedFalse(carId, Pageables.stable(pageable));
        }

        Page<RentalResponse> result = assembler.toPage(rentals);
        log.info("Retrieved {} rentals for car: {}", result.getTotalElements(), carId);
        return result;
    }
}
