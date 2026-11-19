package com.akif.rental.internal.scheduler;

import com.akif.payment.api.PaymentDto;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.api.PaymentService;
import com.akif.payment.api.PaymentStatus;
import com.akif.rental.api.RentalCancelledEvent;
import com.akif.rental.api.CancelledBy;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.config.RentalProperties;
import com.akif.rental.internal.repository.RentalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class NoShowRentalScheduler {

    static final String NO_SHOW_REASON = "Vehicle was not picked up after the start date";

    private final Clock clock;
    private final RentalRepository rentalRepository;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher eventPublisher;
    private final RentalProperties rentalProperties;
    private final TransactionTemplate transactionTemplate;

    public NoShowRentalScheduler(
            Clock clock,
            RentalRepository rentalRepository,
            PaymentService paymentService,
            ApplicationEventPublisher eventPublisher,
            RentalProperties rentalProperties,
            PlatformTransactionManager transactionManager) {
        this.clock = clock;
        this.rentalRepository = rentalRepository;
        this.paymentService = paymentService;
        this.eventPublisher = eventPublisher;
        this.rentalProperties = rentalProperties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(cron = "${rental.no-show-cron:0 */30 * * * *}")
    public int releaseNoShows() {
        LocalDate cutoff = LocalDate.now(clock).minusDays(rentalProperties.getNoShowDays());
        List<Long> candidates = rentalRepository.findConfirmedIdsStartingOnOrBefore(cutoff);
        int released = 0;
        for (Long rentalId : candidates) {
            try {
                Boolean done = transactionTemplate.execute(status -> releaseIfNoShow(rentalId, cutoff));
                if (Boolean.TRUE.equals(done)) {
                    released++;
                }
            } catch (RuntimeException e) {
                log.error("Could not release no-show rental {}: {}", rentalId, e.getMessage());
            }
        }
        if (released > 0) {
            log.info("Released {} no-show rental(s) and their authorisation holds", released);
        }
        return released;
    }

    private boolean releaseIfNoShow(Long rentalId, LocalDate cutoff) {
        Optional<Rental> locked = rentalRepository.findByIdForUpdate(rentalId);
        if (locked.isEmpty()) {
            return false;
        }
        Rental rental = locked.get();
        if (rental.getStatus() != RentalStatus.CONFIRMED || rental.getStartDate().isAfter(cutoff)) {
            return false;
        }
        Optional<PaymentDto> payment = paymentService.getPaymentByRentalId(rentalId);
        if (payment.isEmpty() || payment.get().status() != PaymentStatus.AUTHORIZED) {
            return false;
        }
        PaymentResult released = paymentService.cancelPayment(payment.get().id());
        if (!released.success()) {
            log.warn("Could not release the hold of no-show rental {}: {}", rentalId, released.message());
            return false;
        }

        rental.cancel(NO_SHOW_REASON, CancelledBy.SYSTEM);
        rentalRepository.save(rental);
        Instant now = Instant.now(clock);
        eventPublisher.publishEvent(new RentalCancelledEvent(
                rental.getId(),
                rental.getUserEmail(),
                now,
                now,
                "Cancelled automatically. Reason: " + NO_SHOW_REASON,
                true,
                payment.get().amount(),
                payment.get().transactionId()));
        return true;
    }
}
