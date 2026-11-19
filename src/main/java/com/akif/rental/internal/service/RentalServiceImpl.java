package com.akif.rental.internal.service;

import com.akif.auth.api.UserRentalStatisticsDto;
import com.akif.payment.api.PaymentIntentResult;
import com.akif.payment.api.PaymentSummary;
import com.akif.rental.api.RentalRequest;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.api.RentalService;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.api.RentalSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RentalServiceImpl implements RentalService {

    private final RentalBookingService booking;
    private final RentalLifecycleService lifecycle;
    private final RentalQueryService queries;

    @Override
    public RentalResponse requestRental(RentalRequest request, Long userId) {
        return booking.requestRental(request, userId);
    }

    @Override
    public RentalResponse confirmRental(Long rentalId, String notes) {
        return lifecycle.confirmRental(rentalId, notes);
    }

    @Override
    public RentalResponse pickupRental(Long rentalId, String pickupNotes) {
        return lifecycle.pickupRental(rentalId, pickupNotes);
    }

    @Override
    public RentalResponse returnRental(Long rentalId, String returnNotes) {
        return lifecycle.returnRental(rentalId, returnNotes);
    }

    @Override
    public RentalResponse earlyReturnRental(Long rentalId, String returnNotes) {
        return lifecycle.earlyReturnRental(rentalId, returnNotes);
    }

    @Override
    public RentalResponse cancelRental(Long rentalId, Long userId, String reason) {
        return lifecycle.cancelRental(rentalId, userId, reason);
    }

    @Override
    public void incrementDamageReportCount(Long rentalId) {
        lifecycle.incrementDamageReportCount(rentalId);
    }

    @Override
    public Page<RentalResponse> getMyRentals(Long userId, Pageable pageable) {
        return queries.getMyRentals(userId, pageable);
    }

    @Override
    public Page<RentalResponse> getAllRentals(java.util.Collection<RentalStatus> statuses, Pageable pageable) {
        return queries.getAllRentals(statuses, pageable);
    }

    @Override
    public RentalResponse getRentalById(Long id, Long userId) {
        return queries.getRentalById(id, userId);
    }

    @Override
    public PaymentIntentResult createPaymentIntent(Long rentalId, Long userId) {
        return queries.createPaymentIntent(rentalId, userId);
    }

    @Override
    public PaymentSummary getRentalPayment(Long rentalId, Long userId) {
        return queries.getRentalPayment(rentalId, userId);
    }

    @Override
    public RentalSummaryDto getRentalSummaryById(Long rentalId) {
        return queries.getRentalSummaryById(rentalId);
    }

    @Override
    public int countByStatus(RentalStatus status) {
        return queries.countByStatus(status);
    }

    @Override
    public int countTodaysPickups() {
        return queries.countTodaysPickups();
    }

    @Override
    public int countTodaysReturns() {
        return queries.countTodaysReturns();
    }

    @Override
    public int countOverdueRentals() {
        return queries.countOverdueRentals();
    }

    @Override
    public Set<Long> findCarIdsInUse() {
        return queries.findCarIdsInUse();
    }

    @Override
    public Set<Long> findCarIdsReservedOn(LocalDate date) {
        return queries.findCarIdsReservedOn(date);
    }

    @Override
    public Page<RentalResponse> findPendingApprovals(Pageable pageable) {
        return queries.findPendingApprovals(pageable);
    }

    @Override
    public Page<RentalResponse> findTodaysPickups(Pageable pageable) {
        return queries.findTodaysPickups(pageable);
    }

    @Override
    public Page<RentalResponse> findTodaysReturns(Pageable pageable) {
        return queries.findTodaysReturns(pageable);
    }

    @Override
    public Page<RentalResponse> findOverdueRentals(Pageable pageable) {
        return queries.findOverdueRentals(pageable);
    }

    @Override
    public BigDecimal getAverageRentalDurationDays(LocalDate startDate, LocalDate endDate) {
        return queries.getAverageRentalDurationDays(startDate, endDate);
    }

    @Override
    public UserRentalStatisticsDto getUserStatistics(Long userId) {
        return queries.getUserStatistics(userId);
    }

    @Override
    public Page<RentalResponse> getUserRentals(Long userId, RentalStatus status, Pageable pageable) {
        return queries.getUserRentals(userId, status, pageable);
    }

    @Override
    public Page<RentalResponse> getCarRentals(Long carId, RentalStatus status, Pageable pageable) {
        return queries.getCarRentals(carId, status, pageable);
    }
}
