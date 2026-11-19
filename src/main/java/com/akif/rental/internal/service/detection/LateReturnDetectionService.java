package com.akif.rental.internal.service.detection;

import com.akif.rental.domain.enums.LateReturnStatus;
import com.akif.rental.domain.model.Rental;

import java.time.Instant;

public interface LateReturnDetectionService {

    void detectLateReturns();

    LateReturnStatus calculateLateStatus(Rental rental, Instant currentTime);

    long calculateLateHours(Rental rental, Instant currentTime);

    long calculateLateDays(Rental rental, Instant currentTime);
}
