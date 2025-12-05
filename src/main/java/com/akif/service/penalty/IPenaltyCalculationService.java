package com.akif.service.penalty;

import com.akif.dto.penalty.PenaltyResult;
import com.akif.model.Rental;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface IPenaltyCalculationService {

    PenaltyResult calculatePenalty(Rental rental, LocalDateTime returnTime);

    BigDecimal calculateHourlyPenalty(BigDecimal dailyRate, int lateHours);

    BigDecimal calculateDailyPenalty(BigDecimal dailyRate, int lateDays);

    BigDecimal applyPenaltyCap(BigDecimal penalty, BigDecimal dailyRate);
}
