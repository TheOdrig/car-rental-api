package com.akif.car.api;

import java.time.LocalDate;

public record CarBooking(Long carId, LocalDate startDate, LocalDate endDate) {
}
