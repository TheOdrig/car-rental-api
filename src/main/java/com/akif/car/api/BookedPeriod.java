package com.akif.car.api;

import java.time.LocalDate;

public record BookedPeriod(Long bookingId, LocalDate startDate, LocalDate endDate) {
}
