package com.akif.rental.internal.scheduler;

import java.time.Duration;
import com.akif.rental.api.CancelledBy;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.config.RentalProperties;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.rental.api.RentalCancelledEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class RentalRequestExpiryScheduler {

    static final String EXPIRY_REASON = "Request expired without approval";

    private final Clock clock;
    private final RentalRepository rentalRepository;
    private final RentalProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public RentalRequestExpiryScheduler(
            Clock clock,
            RentalRepository rentalRepository,
            RentalProperties properties,
            ApplicationEventPublisher eventPublisher,
            PlatformTransactionManager transactionManager) {
        this.clock = clock;
        this.rentalRepository = rentalRepository;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(cron = "${rental.request-expiry-cron:0 */10 * * * *}")
    public int expireStaleRequests() {
        Instant now = Instant.now(clock);
        Instant cutoff = now.minus(Duration.ofHours(properties.requestExpiryHours()));
        List<Rental> stale = rentalRepository.findStaleRequests(RentalStatus.REQUESTED, cutoff);
        int expired = 0;
        for (Rental candidate : stale) {
            Long rentalId = candidate.getId();
            try {
                Boolean done = transactionTemplate.execute(status -> expire(rentalId, now));
                if (Boolean.TRUE.equals(done)) {
                    expired++;
                }
            } catch (RuntimeException e) {
                log.error("Could not expire rental request {}: {}", rentalId, e.getMessage());
            }
        }
        if (expired > 0) {
            log.info("Expired {} stale rental request(s) created before {}", expired, cutoff);
        }
        return expired;
    }

    private boolean expire(Long rentalId, Instant now) {
        Optional<Rental> locked = rentalRepository.findByIdForUpdate(rentalId);
        if (locked.isEmpty() || locked.get().getStatus() != RentalStatus.REQUESTED) {
            return false;
        }
        Rental rental = locked.get();
        rental.cancel(EXPIRY_REASON, CancelledBy.SYSTEM);
        rentalRepository.save(rental);
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
