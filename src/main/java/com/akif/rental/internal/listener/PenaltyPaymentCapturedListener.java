package com.akif.rental.internal.listener;

import com.akif.payment.api.PenaltyPaymentCapturedEvent;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PenaltyPaymentCapturedListener {

    private final RentalRepository rentalRepository;

    @ApplicationModuleListener
    public void onPenaltyPaymentCaptured(PenaltyPaymentCapturedEvent event) {
        rentalRepository.findByIdForUpdate(event.rentalId()).ifPresent(rental -> markPaid(rental, event));
    }

    private void markPaid(Rental rental, PenaltyPaymentCapturedEvent event) {
        if (Boolean.TRUE.equals(rental.getPenaltyPaid())) {
            return;
        }
        rental.recordPenaltyPayment(true);
        rentalRepository.save(rental);
        log.info("Penalty of rental {} recorded as paid after capture of payment {}", rental.getId(), event.paymentId());
    }
}
