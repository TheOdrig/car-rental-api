package com.akif.rental.internal.scheduler;

import java.time.Duration;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.config.RentalProperties;
import com.akif.rental.internal.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import com.akif.rental.api.RentalCancelledEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RentalRequestExpiryScheduler {

    static final String EXPIRY_REASON = "Request expired without approval";

    private final Clock clock;
    private final RentalRepository rentalRepository;
    private final RentalProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "${rental.request-expiry-cron:0 */10 * * * *}")
    @Transactional
    public int expireStaleRequests() {
        Instant now = Instant.now(clock);
        Instant cutoff = now.minus(Duration.ofHours(properties.getRequestExpiryHours()));
        List<Rental> stale = rentalRepository.findStaleRequests(RentalStatus.REQUESTED, cutoff);
        for (Rental rental : stale) {
            rental.cancel(EXPIRY_REASON);
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
        }
        if (!stale.isEmpty()) {
            log.info("Expired {} stale rental request(s) created before {}", stale.size(), cutoff);
        }
        return stale.size();
    }
}
