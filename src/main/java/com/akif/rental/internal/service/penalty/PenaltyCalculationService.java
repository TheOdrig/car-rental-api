package com.akif.rental.internal.service.penalty;

import com.akif.rental.internal.dto.penalty.PenaltyResult;
import com.akif.rental.domain.model.Rental;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PenaltyCalculationService {

    PenaltyResult calculatePenalty(Rental rental, LocalDateTime returnTime);

    BigDecimal calculateHourlyPenalty(BigDecimal dailyRate, int lateHours, CurrencyType currency);

    BigDecimal calculateDailyPenalty(BigDecimal dailyRate, int lateDays, CurrencyType currency);

    BigDecimal applyPenaltyCap(BigDecimal penalty, BigDecimal dailyRate, CurrencyType currency);
}
