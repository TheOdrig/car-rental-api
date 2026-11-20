package com.akif.rental.internal.service.penalty.impl;

import com.akif.shared.time.LateHours;
import com.akif.shared.time.RentalDays;
import com.akif.rental.internal.config.PenaltyConfig;
import com.akif.rental.internal.dto.penalty.PenaltyResult;
import com.akif.rental.domain.enums.LateReturnStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.service.penalty.PenaltyCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PenaltyCalculationServiceImpl implements PenaltyCalculationService {

    private final PenaltyConfig penaltyConfig;

    @Override
    public PenaltyResult calculatePenalty(Rental rental, Instant returnTime) {
        log.debug("Calculating penalty for rental: {}, return time: {}", rental.getId(), returnTime);

        BigDecimal dailyRate = rental.getDailyPrice();
        long gracePeriodMinutes = penaltyConfig.gracePeriodMinutes();
        int lateHours = (int) LateHours.between(rental.getEndDate(), returnTime, gracePeriodMinutes);

        if (lateHours <= 0) {
            log.debug("Rental {} is within grace period. No penalty applied.", rental.getId());
            return new PenaltyResult(
                BigDecimal.ZERO,
                dailyRate,
                0,
                0,
                LateReturnStatus.GRACE_PERIOD,
                "Within grace period - no penalty",
                false
            );
        }

        int lateDays = RentalDays.lateDays(lateHours);

        LateReturnStatus status;
        if (lateHours >= penaltyConfig.severelyLateThresholdHours()) {
            status = LateReturnStatus.SEVERELY_LATE;
        } else {
            status = LateReturnStatus.LATE;
        }

        BigDecimal penalty;
        String breakdown;
        
        if (lateHours <= penaltyConfig.hourlyTierMaxHours()) {
            penalty = calculateHourlyPenalty(dailyRate, lateHours, rental.getCurrency());
            breakdown = String.format("Hourly penalty: %d hours × %.0f%% × %s = %s",
                lateHours,
                penaltyConfig.hourlyPenaltyRate().multiply(new BigDecimal("100")),
                dailyRate,
                penalty);
        } else if (lateHours <= penaltyConfig.singleDayTierMaxHours()) {
            penalty = calculateDailyPenalty(dailyRate, 1, rental.getCurrency());
            breakdown = String.format("Daily penalty: 1 day × %.0f%% × %s = %s",
                penaltyConfig.dailyPenaltyRate().multiply(new BigDecimal("100")),
                dailyRate,
                penalty);
        } else {
            penalty = calculateDailyPenalty(dailyRate, lateDays, rental.getCurrency());
            breakdown = String.format("Daily penalty: %d days × %.0f%% × %s = %s",
                lateDays,
                penaltyConfig.dailyPenaltyRate().multiply(new BigDecimal("100")),
                dailyRate,
                penalty);
        }

        BigDecimal originalPenalty = penalty;
        penalty = applyPenaltyCap(penalty, dailyRate, rental.getCurrency());
        boolean cappedAtMax = penalty.compareTo(originalPenalty) < 0;
        
        if (cappedAtMax) {
            breakdown += String.format(" (capped at %.0fx daily rate: %s)",
                penaltyConfig.penaltyCapMultiplier(),
                penalty);
        }
        
        log.info("Penalty calculated for rental {}: {} (status: {}, late hours: {}, capped: {})",
            rental.getId(), penalty, status, lateHours, cappedAtMax);
        
        return new PenaltyResult(
            penalty,
            dailyRate,
            lateHours,
            lateDays,
            status,
            breakdown,
            cappedAtMax
        );
    }

    @Override
    public BigDecimal calculateHourlyPenalty(BigDecimal dailyRate, int lateHours, CurrencyType currency) {
        if (lateHours <= 0 || lateHours > penaltyConfig.hourlyTierMaxHours()) {
            throw new IllegalArgumentException(
                "Hourly penalty applies only for 1-" + penaltyConfig.hourlyTierMaxHours() + " hours late");
        }

        BigDecimal penalty = dailyRate
            .multiply(penaltyConfig.hourlyPenaltyRate())
            .multiply(new BigDecimal(lateHours))
            .setScale(currency.getDecimalPlaces(), RoundingMode.HALF_UP);
        
        log.debug("Hourly penalty calculated: {} hours × {}% × {} = {}",
            lateHours, penaltyConfig.hourlyPenaltyRate(), dailyRate, penalty);
        
        return penalty;
    }

    @Override
    public BigDecimal calculateDailyPenalty(BigDecimal dailyRate, int lateDays, CurrencyType currency) {
        if (lateDays <= 0) {
            throw new IllegalArgumentException("Daily penalty applies only for 1+ days late");
        }

        BigDecimal penalty = dailyRate
            .multiply(penaltyConfig.dailyPenaltyRate())
            .multiply(new BigDecimal(lateDays))
            .setScale(currency.getDecimalPlaces(), RoundingMode.HALF_UP);
        
        log.debug("Daily penalty calculated: {} days × {}% × {} = {}",
            lateDays, penaltyConfig.dailyPenaltyRate(), dailyRate, penalty);
        
        return penalty;
    }

    @Override
    public BigDecimal applyPenaltyCap(BigDecimal penalty, BigDecimal dailyRate, CurrencyType currency) {
        BigDecimal maxPenalty = dailyRate
            .multiply(penaltyConfig.penaltyCapMultiplier())
            .setScale(currency.getDecimalPlaces(), RoundingMode.HALF_UP);
        
        if (penalty.compareTo(maxPenalty) > 0) {
            log.debug("Penalty {} exceeds cap {}. Applying cap.", penalty, maxPenalty);
            return maxPenalty;
        }
        
        return penalty;
    }
}
