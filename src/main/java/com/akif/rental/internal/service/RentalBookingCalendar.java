package com.akif.rental.internal.service;

import com.akif.car.api.BookedPeriod;
import com.akif.car.api.CarBooking;
import com.akif.car.api.CarBookingCalendar;
import com.akif.car.api.CarCurrentRental;
import com.akif.car.api.CarOccupancy;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentalBookingCalendar implements CarBookingCalendar {

    private static final List<RentalStatus> COMMITTED_STATUSES = List.of(RentalStatus.CONFIRMED, RentalStatus.IN_USE);

    private static final List<RentalStatus> BLOCKING_STATUSES = List.of(RentalStatus.REQUESTED, RentalStatus.CONFIRMED, RentalStatus.IN_USE);

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

    @Override
    public List<CarBooking> findCommittedBookings(LocalDate startDate, LocalDate endDate) {
        return rentalRepository.findCommittedBookings(COMMITTED_STATUSES, startDate, endDate).stream()
                .map(row -> new CarBooking((Long) row[0], (Long) row[1], (LocalDate) row[2], (LocalDate) row[3]))
                .toList();
    }

    @Override
    public Map<Long, CarCurrentRental> findCurrentRentals(LocalDate today) {
        Map<Long, CarCurrentRental> current = new LinkedHashMap<>();
        for (Object[] row : rentalRepository.findCurrentRentalRows(today)) {
            Long carId = (Long) row[0];
            CarOccupancy occupancy = row[2] == RentalStatus.IN_USE ? CarOccupancy.IN_USE : CarOccupancy.RESERVED;
            current.putIfAbsent(carId, new CarCurrentRental(carId, (Long) row[1], occupancy));
        }
        return current;
    }
}
