package com.akif.car.internal.dto.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record PricingContext(
    Long carId,
    BigDecimal basePrice,
    LocalDate startDate,
    LocalDate endDate,
    LocalDate bookingDate,
    int rentalDays,
    int leadTimeDays,
    String carCategory,
    Long excludedBookingId,
    Integer fleetOccupancyPercent
) {

    public PricingContext(
            Long carId,
            BigDecimal basePrice,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate bookingDate,
            int rentalDays,
            int leadTimeDays,
            String carCategory,
            Long excludedBookingId) {
        this(carId, basePrice, startDate, endDate, bookingDate, rentalDays, leadTimeDays, carCategory,
                excludedBookingId, null);
    }

    public PricingContext(
            Long carId,
            BigDecimal basePrice,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate bookingDate,
            int rentalDays,
            int leadTimeDays,
            String carCategory) {
        this(carId, basePrice, startDate, endDate, bookingDate, rentalDays, leadTimeDays, carCategory, null, null);
    }

    public PricingContext withFleetOccupancyPercent(Integer percent) {
        return new PricingContext(carId, basePrice, startDate, endDate, bookingDate, rentalDays, leadTimeDays,
                carCategory, excludedBookingId, percent);
    }

    public static PricingContext create(
            Long carId,
            BigDecimal basePrice,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate bookingDate,
            String carCategory) {

        int rentalDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        int leadTimeDays = (int) ChronoUnit.DAYS.between(bookingDate, startDate);

        return new PricingContext(
            carId,
            basePrice,
            startDate,
            endDate,
            bookingDate,
            rentalDays,
            leadTimeDays,
            carCategory,
            null,
            null
        );
    }
}
