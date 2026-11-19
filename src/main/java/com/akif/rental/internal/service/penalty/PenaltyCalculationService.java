package com.akif.rental.internal.service.penalty;

import com.akif.rental.internal.dto.penalty.PenaltyResult;
import com.akif.rental.domain.model.Rental;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;

public interface PenaltyCalculationService {

    PenaltyResult calculatePenalty(Rental rental, Instant returnTime);

    BigDecimal calculateHourlyPenalty(BigDecimal dailyRate, int lateHours, CurrencyType currency);

    BigDecimal calculateDailyPenalty(BigDecimal dailyRate, int lateDays, CurrencyType currency);

    BigDecimal applyPenaltyCap(BigDecimal penalty, BigDecimal dailyRate, CurrencyType currency);
}
