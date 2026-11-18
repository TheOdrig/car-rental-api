package com.akif.car.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface CarBookingCalendar {

    long countActiveBookings(Long carId, LocalDate startDate, LocalDate endDate);

    List<BookedPeriod> findBookedPeriods(Long carId, LocalDate startDate, LocalDate endDate);

    Set<Long> findBookedCarIds(LocalDate startDate, LocalDate endDate);
}
