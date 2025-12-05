package com.akif.service.detection;

import com.akif.enums.LateReturnStatus;
import com.akif.model.Rental;

import java.time.LocalDateTime;

public interface ILateReturnDetectionService {

    void detectLateReturns();

    LateReturnStatus calculateLateStatus(Rental rental, LocalDateTime currentTime);

    long calculateLateHours(Rental rental, LocalDateTime currentTime);

    long calculateLateDays(Rental rental, LocalDateTime currentTime);
}
