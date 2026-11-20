package com.akif.rental.internal.service.penalty.impl;

import com.akif.rental.api.LateHoursPolicy;
import com.akif.rental.internal.config.PenaltyConfig;
import com.akif.shared.time.LateHours;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class LateHoursPolicyImpl implements LateHoursPolicy {

    private final PenaltyConfig penaltyConfig;

    @Override
    public long lateHours(LocalDate endDate, Instant now) {
        return LateHours.between(endDate, now, penaltyConfig.gracePeriodMinutes());
    }

    @Override
    public long severelyLateThresholdHours() {
        return penaltyConfig.severelyLateThresholdHours();
    }
}
