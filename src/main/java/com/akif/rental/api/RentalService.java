package com.akif.rental.api;

import com.akif.rental.domain.enums.RentalStatus;
import com.akif.rental.internal.dto.request.RentalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RentalService {

    RentalResponse requestRental(RentalRequest request, String username);

    RentalResponse confirmRental(Long rentalId);

    RentalResponse pickupRental(Long rentalId, String pickupNotes);

    RentalResponse returnRental(Long rentalId, String returnNotes);

    RentalResponse cancelRental(Long rentalId, String username);

    Page<RentalResponse> getMyRentals(String username, Pageable pageable);

    Page<RentalResponse> getAllRentals(Pageable pageable);

    RentalResponse getRentalById(Long id, String username);

    RentalSummaryDto getRentalSummaryById(Long rentalId);

    void incrementDamageReportCount(Long rentalId);


    int countByStatus(RentalStatus status);

    int countTodaysPickups();

    int countTodaysReturns();

    int countOverdueRentals();

    Page<RentalResponse> findPendingApprovals(Pageable pageable);

    Page<RentalResponse> findTodaysPickups(Pageable pageable);

    Page<RentalResponse> findTodaysReturns(Pageable pageable);

    Page<RentalResponse> findOverdueRentals(Pageable pageable);

    BigDecimal sumCollectedPenaltyRevenue(LocalDate startDate, LocalDate endDate);

    BigDecimal getAverageRentalDurationDays(LocalDate startDate, LocalDate endDate);
}
