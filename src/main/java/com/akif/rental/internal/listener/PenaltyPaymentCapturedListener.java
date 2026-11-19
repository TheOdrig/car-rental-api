package com.akif.rental.internal.listener;

import com.akif.payment.api.PaymentService;
import com.akif.payment.api.PaymentStatus;
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
    private final PaymentService paymentService;

    @ApplicationModuleListener
    public void onPenaltyPaymentCaptured(PenaltyPaymentCapturedEvent event) {
        rentalRepository.findByIdForUpdate(event.rentalId()).ifPresent(rental -> markPaid(rental, event));
    }

    private void markPaid(Rental rental, PenaltyPaymentCapturedEvent event) {
        if (Boolean.TRUE.equals(rental.getPenaltyPaid())) {
            return;
        }
        if (rental.getPenaltyAmount() == null || rental.getPenaltyAmount().signum() <= 0) {
            log.info("Penalty of rental {} is waived or zero, capture of payment {} is not recorded as paid",
                    rental.getId(), event.paymentId());
            return;
        }
        boolean stillCaptured = paymentService.getPaymentById(event.paymentId())
                .map(payment -> payment.status() == PaymentStatus.CAPTURED)
                .orElse(false);
        if (!stillCaptured) {
            log.info("Payment {} is no longer captured, penalty of rental {} is not recorded as paid",
                    event.paymentId(), rental.getId());
            return;
        }
        rental.recordPenaltyPayment(true);
        rentalRepository.save(rental);
        log.info("Penalty of rental {} recorded as paid after capture of payment {}", rental.getId(), event.paymentId());
    }
}
