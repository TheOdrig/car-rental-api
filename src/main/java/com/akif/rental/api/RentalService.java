package com.akif.rental.api;

import com.akif.auth.api.UserRentalStatisticsProvider;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.api.RentalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RentalService extends UserRentalStatisticsProvider {

    RentalResponse requestRental(RentalRequest request, Long userId);

    RentalResponse confirmRental(Long rentalId, String notes);

    RentalResponse pickupRental(Long rentalId, String pickupNotes);

    RentalResponse returnRental(Long rentalId, String returnNotes);

    RentalResponse earlyReturnRental(Long rentalId, String returnNotes);

    EarlyReturnPreview previewEarlyReturn(Long rentalId);

    RentalResponse cancelRental(Long rentalId, Long userId, String reason);

    Page<RentalResponse> getMyRentals(Long userId, Pageable pageable);

    Page<RentalResponse> getAllRentals(java.util.Collection<RentalStatus> statuses, Pageable pageable);

    RentalResponse getRentalById(Long id, Long userId);

    com.akif.payment.api.PaymentIntentResult createPaymentIntent(Long rentalId, Long userId);

    com.akif.payment.api.PaymentSummary getRentalPayment(Long rentalId, Long userId);

    RentalSummaryDto getRentalSummaryById(Long rentalId);

    void incrementDamageReportCount(Long rentalId);

    int countByStatus(RentalStatus status);

    int countTodaysPickups();

    int countTodaysReturns();

    int countOverdueRentals();

    java.util.Set<Long> findCarIdsInUse();

    java.util.Set<Long> findCarIdsReservedOn(LocalDate date);

    Page<RentalResponse> findPendingApprovals(Pageable pageable);

    Page<RentalResponse> findTodaysPickups(Pageable pageable);

    Page<RentalResponse> findTodaysReturns(Pageable pageable);

    Page<RentalResponse> findOverdueRentals(Pageable pageable);

    BigDecimal getAverageRentalDurationDays(LocalDate startDate, LocalDate endDate);

    Page<RentalResponse> getUserRentals(Long userId, RentalStatus status, Pageable pageable);

    Page<RentalResponse> getCarRentals(Long carId, RentalStatus status, Pageable pageable);
}
