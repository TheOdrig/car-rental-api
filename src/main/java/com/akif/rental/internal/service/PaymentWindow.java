package com.akif.rental.internal.service;

import com.akif.rental.internal.config.RentalProperties;
import com.akif.rental.internal.exception.PaymentWindowNotOpenException;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.time.Clock;
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

    public boolean hasOpened(LocalDate startDate) {
        return !LocalDate.now(clock).isBefore(opensOn(startDate));
    }

    public void requireOpen(LocalDate startDate) {
        if (!hasOpened(startDate)) {
            throw new PaymentWindowNotOpenException(opensOn(startDate));
        }
    }
}
