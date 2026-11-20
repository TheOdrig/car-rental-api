package com.akif.shared.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public final class UtcDates {

    private UtcDates() {
    }

    public static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    public static Instant endOfDayWholeSecond(LocalDate date) {
        return date.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
    }

    public static LocalDate toDate(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }
}
