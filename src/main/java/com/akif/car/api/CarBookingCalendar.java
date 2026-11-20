package com.akif.car.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CarBookingCalendar {

    long countActiveBookings(Long carId, LocalDate startDate, LocalDate endDate);

    List<BookedPeriod> findBookedPeriods(Long carId, LocalDate startDate, LocalDate endDate);

    Set<Long> findBookedCarIds(LocalDate startDate, LocalDate endDate);

    List<CarBooking> findCommittedBookings(LocalDate startDate, LocalDate endDate);

    Map<Long, CarCurrentRental> findCurrentRentals(LocalDate today);
}
