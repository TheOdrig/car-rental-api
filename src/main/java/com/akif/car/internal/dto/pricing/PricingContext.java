package com.akif.car.internal.dto.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.akif.shared.time.RentalDays;

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

        int rentalDays = RentalDays.inclusiveDays(startDate, endDate);
        int leadTimeDays = RentalDays.daysBetween(bookingDate, startDate);

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
