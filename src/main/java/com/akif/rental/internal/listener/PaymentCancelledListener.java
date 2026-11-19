package com.akif.rental.internal.listener;

import com.akif.payment.api.PaymentCancelledEvent;
import com.akif.rental.api.PaymentRetryRequestedEvent;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.config.RentalProperties;
import com.akif.rental.internal.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCancelledListener {

    private final Clock clock;
    private final RentalRepository rentalRepository;
    private final RentalProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    @ApplicationModuleListener
    public void onPaymentCancelled(PaymentCancelledEvent event) {
        Optional<Rental> found = rentalRepository.findByIdAndIsDeletedFalse(event.rentalId());
        if (found.isEmpty()) {
            return;
        }
        Rental rental = found.get();
        LocalDate today = LocalDate.now(clock);
        LocalDate opensOn = rental.getStartDate().minusDays(properties.getPaymentWindowDays());
        boolean windowOpen = !today.isBefore(opensOn) && today.isBefore(rental.getStartDate());
        if (rental.getStatus() != RentalStatus.CONFIRMED || !windowOpen) {
            log.info("Payment cancelled for rental {} but no retry is requested (status={}, windowOpen={})",
                    rental.getId(), rental.getStatus(), windowOpen);
            return;
        }
        eventPublisher.publishEvent(new PaymentRetryRequestedEvent(
                rental.getId(),
                rental.getUserEmail(),
                Instant.now(clock),
                rental.getCarBrand(),
                rental.getCarModel(),
                rental.getStartDate(),
                event.amount(),
                event.currency()));
    }
}
