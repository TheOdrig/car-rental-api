package com.akif.rental.internal.service;

import com.akif.car.api.BookedPeriod;
import com.akif.car.api.CarBookingCalendar;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentalBookingCalendar implements CarBookingCalendar {

    private static final List<RentalStatus> BLOCKING_STATUSES = List.of(RentalStatus.CONFIRMED, RentalStatus.IN_USE);

    private final RentalRepository rentalRepository;

    @Override
    public long countActiveBookings(Long carId, LocalDate startDate, LocalDate endDate) {
        return rentalRepository.countOverlappingRentals(carId, startDate, endDate);
    }

    @Override
    public List<BookedPeriod> findBookedPeriods(Long carId, LocalDate startDate, LocalDate endDate) {
        List<Rental> rentals = rentalRepository.findOverlappingRentalsForCar(
                carId, startDate, endDate, BLOCKING_STATUSES);
        return rentals.stream()
                .map(rental -> new BookedPeriod(rental.getId(), rental.getStartDate(), rental.getEndDate()))
                .toList();
    }

    @Override
    public Set<Long> findBookedCarIds(LocalDate startDate, LocalDate endDate) {
        return new HashSet<>(rentalRepository.findBookedCarIds(BLOCKING_STATUSES, startDate, endDate));
    }
}
