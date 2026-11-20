package com.akif.rental.api;

import java.time.Instant;
import java.time.LocalDate;

public interface LateHoursPolicy {

    long lateHours(LocalDate endDate, Instant now);
}
