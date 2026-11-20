package com.akif.shared.time;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

public final class RentalDays {

    private static final int HOURS_PER_DAY = 24;

    private RentalDays() {
    }

    public static int inclusiveDays(LocalDate startDate, LocalDate endDate) {
        return Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate) + 1);
    }

    public static int daysBetween(LocalDate from, LocalDate to) {
        return Math.toIntExact(ChronoUnit.DAYS.between(from, to));
    }

    public static int elapsedInclusiveDays(LocalDate startDate, LocalDate today) {
        return today.isBefore(startDate) ? 0 : inclusiveDays(startDate, today);
    }

    public static int lateDays(long lateHours) {
        if (lateHours <= 0) {
            return 0;
        }
        return (int) ((lateHours + HOURS_PER_DAY - 1) / HOURS_PER_DAY);
    }

    public static Stream<LocalDate> eachDay(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            return Stream.empty();
        }
        return startDate.datesUntil(endDate.plusDays(1));
    }
}
