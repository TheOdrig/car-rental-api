package com.akif.car.api;

import java.time.LocalDate;

public record CarBooking(Long bookingId, Long carId, LocalDate startDate, LocalDate endDate) {

    public CarBooking(Long carId, LocalDate startDate, LocalDate endDate) {
        this(null, carId, startDate, endDate);
    }
}
