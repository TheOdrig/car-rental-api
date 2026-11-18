package com.akif.rental.internal.scheduler;

import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.config.RentalProperties;
import com.akif.rental.internal.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RentalRequestExpiryScheduler {

    static final String EXPIRY_REASON = "Request expired without approval";

    private final Clock clock;
    private final RentalRepository rentalRepository;
    private final RentalProperties properties;

    @Scheduled(cron = "${rental.request-expiry-cron:0 */10 * * * *}")
    @Transactional
    public int expireStaleRequests() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusHours(properties.getRequestExpiryHours());
        List<Rental> stale = rentalRepository.findStaleRequests(RentalStatus.REQUESTED, cutoff);
        for (Rental rental : stale) {
            rental.cancel(EXPIRY_REASON);
            rentalRepository.save(rental);
        }
        if (!stale.isEmpty()) {
            log.info("Expired {} stale rental request(s) created before {}", stale.size(), cutoff);
        }
        return stale.size();
    }
}
