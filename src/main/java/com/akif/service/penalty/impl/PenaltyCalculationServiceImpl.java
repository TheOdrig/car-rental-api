package com.akif.service.penalty.impl;

import com.akif.config.PenaltyConfig;
import com.akif.dto.penalty.PenaltyResult;
import com.akif.enums.LateReturnStatus;
import com.akif.model.Rental;
import com.akif.service.penalty.IPenaltyCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PenaltyCalculationServiceImpl implements IPenaltyCalculationService {

    private final PenaltyConfig penaltyConfig;

    @Override
    public PenaltyResult calculatePenalty(Rental rental, LocalDateTime returnTime) {
        log.debug("Calculating penalty for rental: {}, return time: {}", rental.getId(), returnTime);

        BigDecimal dailyRate = rental.getDailyPrice();
        LocalDateTime endDateTime = rental.getEndDate().atTime(23, 59, 59);

        Duration lateDuration = Duration.between(endDateTime, returnTime);
        long totalMinutesLate = lateDuration.toMinutes();

        long gracePeriodMinutes = penaltyConfig.getGracePeriodMinutes();
        long minutesAfterGrace = totalMinutesLate - gracePeriodMinutes;

        if (minutesAfterGrace <= 0) {
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

        int lateHours = (int) Math.ceil(minutesAfterGrace / 60.0);
        int lateDays = lateHours / 24;

        LateReturnStatus status;
        if (lateHours >= penaltyConfig.getSeverelyLateThresholdHours()) {
            status = LateReturnStatus.SEVERELY_LATE;
        } else {
            status = LateReturnStatus.LATE;
        }

        BigDecimal penalty;
        String breakdown;
        
        if (lateHours <= 6) {
            penalty = calculateHourlyPenalty(dailyRate, lateHours);
            breakdown = String.format("Hourly penalty: %d hours × %.0f%% × %s = %s",
                lateHours,
                penaltyConfig.getHourlyPenaltyRate().multiply(new BigDecimal("100")),
                dailyRate,
                penalty);
        } else if (lateHours <= 24) {
            penalty = calculateDailyPenalty(dailyRate, 1);
            breakdown = String.format("Daily penalty: 1 day × %.0f%% × %s = %s",
                penaltyConfig.getDailyPenaltyRate().multiply(new BigDecimal("100")),
                dailyRate,
                penalty);
        } else {
            penalty = calculateDailyPenalty(dailyRate, lateDays);
            breakdown = String.format("Daily penalty: %d days × %.0f%% × %s = %s",
                lateDays,
                penaltyConfig.getDailyPenaltyRate().multiply(new BigDecimal("100")),
                dailyRate,
                penalty);
        }

        BigDecimal originalPenalty = penalty;
        penalty = applyPenaltyCap(penalty, dailyRate);
        boolean cappedAtMax = penalty.compareTo(originalPenalty) < 0;
        
        if (cappedAtMax) {
            breakdown += String.format(" (capped at %.0fx daily rate: %s)",
                penaltyConfig.getPenaltyCapMultiplier(),
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
    public BigDecimal calculateHourlyPenalty(BigDecimal dailyRate, int lateHours) {
        if (lateHours <= 0 || lateHours > 6) {
            throw new IllegalArgumentException("Hourly penalty applies only for 1-6 hours late");
        }

        BigDecimal penalty = dailyRate
            .multiply(penaltyConfig.getHourlyPenaltyRate())
            .multiply(new BigDecimal(lateHours))
            .setScale(2, RoundingMode.HALF_UP);
        
        log.debug("Hourly penalty calculated: {} hours × {}% × {} = {}",
            lateHours, penaltyConfig.getHourlyPenaltyRate(), dailyRate, penalty);
        
        return penalty;
    }

    @Override
    public BigDecimal calculateDailyPenalty(BigDecimal dailyRate, int lateDays) {
        if (lateDays <= 0) {
            throw new IllegalArgumentException("Daily penalty applies only for 1+ days late");
        }

        BigDecimal penalty = dailyRate
            .multiply(penaltyConfig.getDailyPenaltyRate())
            .multiply(new BigDecimal(lateDays))
            .setScale(2, RoundingMode.HALF_UP);
        
        log.debug("Daily penalty calculated: {} days × {}% × {} = {}",
            lateDays, penaltyConfig.getDailyPenaltyRate(), dailyRate, penalty);
        
        return penalty;
    }

    @Override
    public BigDecimal applyPenaltyCap(BigDecimal penalty, BigDecimal dailyRate) {
        BigDecimal maxPenalty = dailyRate
            .multiply(penaltyConfig.getPenaltyCapMultiplier())
            .setScale(2, RoundingMode.HALF_UP);
        
        if (penalty.compareTo(maxPenalty) > 0) {
            log.debug("Penalty {} exceeds cap {}. Applying cap.", penalty, maxPenalty);
            return maxPenalty;
        }
        
        return penalty;
    }
}
