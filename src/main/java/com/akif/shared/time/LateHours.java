package com.akif.shared.time;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

public final class LateHours {

    private LateHours() {
    }

    public static Instant dueAt(LocalDate endDate) {
        return UtcDates.endOfDayWholeSecond(endDate);
    }

    public static long between(LocalDate endDate, Instant now, long graceMinutes) {
        if (endDate == null) {
            return 0;
        }
        long minutesAfterGrace = Duration.between(dueAt(endDate), now).toMinutes() - graceMinutes;
        if (minutesAfterGrace <= 0) {
            return 0;
        }
        return (minutesAfterGrace + 59) / 60;
    }
}
