package com.akif.rental.internal.service;

import com.akif.rental.internal.config.RentalProperties;
import com.akif.rental.internal.exception.PaymentWindowNotOpenException;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import com.akif.shared.time.UtcDates;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class PaymentWindow {

    public static final String OPENS_ON = "paymentOpensOn";

    private final Clock clock;
    private final RentalProperties properties;

    @Named(OPENS_ON)
    public LocalDate opensOn(LocalDate startDate) {
        return startDate == null ? null : startDate.minusDays(properties.getPaymentWindowDays());
    }

    public Instant dueBy(LocalDate startDate, Instant confirmedAt) {
        Instant startOfStartDay = UtcDates.startOfDay(startDate);
        if (confirmedAt == null) {
            return startOfStartDay;
        }
        Instant graceEnds = confirmedAt.plus(Duration.ofHours(properties.getPaymentGraceHours()));
        return graceEnds.isAfter(startOfStartDay) ? graceEnds : startOfStartDay;
    }

    public boolean isOverdue(LocalDate startDate, Instant confirmedAt) {
        return !Instant.now(clock).isBefore(dueBy(startDate, confirmedAt));
    }

    public boolean hasOpened(LocalDate startDate) {
        return !LocalDate.now(clock).isBefore(opensOn(startDate));
    }

    public void requireOpen(LocalDate startDate) {
        if (!hasOpened(startDate)) {
            throw new PaymentWindowNotOpenException(opensOn(startDate));
        }
    }
}
