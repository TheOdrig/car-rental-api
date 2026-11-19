package com.akif.rental.internal.scheduler;

import com.akif.payment.api.PaymentDto;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.api.PaymentService;
import com.akif.payment.api.PaymentStatus;
import com.akif.rental.api.CancelledBy;
import com.akif.rental.api.RentalCancelledEvent;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.rental.internal.service.PaymentWindow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class UnpaidRentalExpiryScheduler {

    static final String EXPIRY_REASON = "Payment not completed before the payment deadline";

    private final Clock clock;
    private final RentalRepository rentalRepository;
    private final PaymentService paymentService;
    private final PaymentWindow paymentWindow;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public UnpaidRentalExpiryScheduler(
            Clock clock,
            RentalRepository rentalRepository,
            PaymentService paymentService,
            PaymentWindow paymentWindow,
            ApplicationEventPublisher eventPublisher,
            PlatformTransactionManager transactionManager) {
        this.clock = clock;
        this.rentalRepository = rentalRepository;
        this.paymentService = paymentService;
        this.paymentWindow = paymentWindow;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(cron = "${rental.payment-expiry-cron:0 */10 * * * *}")
    public int expireUnpaidRentals() {
        LocalDate today = LocalDate.now(clock);
        List<Long> candidates = rentalRepository.findConfirmedIdsStartingOnOrBefore(today);
        int expired = 0;
        for (Long rentalId : candidates) {
            try {
                Boolean done = transactionTemplate.execute(status -> expireIfUnpaid(rentalId, today));
                if (Boolean.TRUE.equals(done)) {
                    expired++;
                }
            } catch (RuntimeException e) {
                log.error("Could not expire unpaid rental {}: {}", rentalId, e.getMessage());
            }
        }
        if (expired > 0) {
            log.info("Cancelled {} unpaid rental(s) whose start date has arrived", expired);
        }
        return expired;
    }

    private boolean expireIfUnpaid(Long rentalId, LocalDate today) {
        Optional<Rental> locked = rentalRepository.findByIdForUpdate(rentalId);
        if (locked.isEmpty()) {
            return false;
        }
        Rental rental = locked.get();
        if (rental.getStatus() != RentalStatus.CONFIRMED || rental.getStartDate().isAfter(today)
                || !paymentWindow.isOverdue(rental.getStartDate(), rental.getConfirmedAt())) {
            return false;
        }
        Optional<PaymentDto> payment = paymentService.getPaymentByRentalId(rentalId);
        if (payment.isPresent()) {
            PaymentStatus paymentStatus = payment.get().status();
            if (paymentStatus == PaymentStatus.AUTHORIZED || paymentStatus == PaymentStatus.CAPTURED) {
                return false;
            }
            if (paymentStatus == PaymentStatus.PENDING || paymentStatus == PaymentStatus.FAILED) {
                PaymentResult released = paymentService.cancelPayment(payment.get().id());
                if (!released.success()) {
                    log.warn("Could not release the unpaid hold of rental {}: {}", rentalId, released.message());
                    return false;
                }
            }
        }

        rental.cancel(EXPIRY_REASON, CancelledBy.SYSTEM);
        rentalRepository.save(rental);
        Instant now = Instant.now(clock);
        eventPublisher.publishEvent(new RentalCancelledEvent(
                rental.getId(),
                rental.getUserEmail(),
                now,
                now,
                "Cancelled automatically. Reason: " + EXPIRY_REASON,
                false,
                BigDecimal.ZERO,
                null));
        return true;
    }
}
