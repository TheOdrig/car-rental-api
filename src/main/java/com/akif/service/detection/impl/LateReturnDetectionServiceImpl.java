package com.akif.service.detection.impl;

import com.akif.config.PenaltyConfig;
import com.akif.enums.LateReturnStatus;
import com.akif.model.Rental;
import com.akif.repository.RentalRepository;
import com.akif.service.detection.ILateReturnDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LateReturnDetectionServiceImpl implements ILateReturnDetectionService {

    private final RentalRepository rentalRepository;
    private final PenaltyConfig penaltyConfig;

    private static final int PAGE_SIZE = 50;

    @Override
    @Transactional
    public void detectLateReturns() {
        log.info("Starting late return detection process");
        
        LocalDate currentDate = LocalDate.now();
        LocalDateTime currentTime = LocalDateTime.now();
        int pageNumber = 0;
        int totalProcessed = 0;
        int totalUpdated = 0;

        try {
            Page<Rental> overdueRentalsPage;
            do {
                Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);
                overdueRentalsPage = rentalRepository.findOverdueRentals(currentDate, pageable);

                for (Rental rental : overdueRentalsPage.getContent()) {
                    totalProcessed++;
                    
                    try {
                        LateReturnStatus newStatus = calculateLateStatus(rental, currentTime);

                        if (rental.getLateReturnStatus() != newStatus) {
                            rental.setLateReturnStatus(newStatus);

                            if (rental.getLateDetectedAt() == null && newStatus != LateReturnStatus.ON_TIME) {
                                rental.setLateDetectedAt(currentTime);
                            }

                            long lateHours = calculateLateHours(rental, currentTime);
                            rental.setLateHours((int) lateHours);
                            
                            rentalRepository.save(rental);
                            totalUpdated++;
                            
                            log.debug("Updated rental {} to status: {}, late hours: {}", 
                                    rental.getId(), newStatus, lateHours);
                        }
                    } catch (Exception e) {
                        log.error("Error processing rental {}: {}", rental.getId(), e.getMessage(), e);
                    }
                }

                pageNumber++;
            } while (overdueRentalsPage.hasNext());

            log.info("Late return detection completed. Processed: {}, Updated: {}", 
                    totalProcessed, totalUpdated);
            
        } catch (Exception e) {
            log.error("Error during late return detection: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public LateReturnStatus calculateLateStatus(Rental rental, LocalDateTime currentTime) {
        if (rental == null || rental.getEndDate() == null) {
            throw new IllegalArgumentException("Rental and end date cannot be null");
        }

        LocalDateTime rentalEndDateTime = rental.getEndDate().atTime(LocalTime.MAX);

        if (!currentTime.isAfter(rentalEndDateTime)) {
            return LateReturnStatus.ON_TIME;
        }

        long minutesLate = Duration.between(rentalEndDateTime, currentTime).toMinutes();

        if (minutesLate <= penaltyConfig.getGracePeriodMinutes()) {
            return LateReturnStatus.GRACE_PERIOD;
        }

        long hoursLate = calculateLateHours(rental, currentTime);

        if (hoursLate >= penaltyConfig.getSeverelyLateThresholdHours()) {
            return LateReturnStatus.SEVERELY_LATE;
        }

        return LateReturnStatus.LATE;
    }

    @Override
    public long calculateLateHours(Rental rental, LocalDateTime currentTime) {
        if (rental == null || rental.getEndDate() == null) {
            return 0;
        }

        LocalDateTime rentalEndDateTime = rental.getEndDate().atTime(LocalTime.MAX);

        if (!currentTime.isAfter(rentalEndDateTime)) {
            return 0;
        }

        long minutesLate = Duration.between(rentalEndDateTime, currentTime).toMinutes();

        long minutesAfterGrace = minutesLate - penaltyConfig.getGracePeriodMinutes();

        if (minutesAfterGrace <= 0) {
            return 0;
        }

        return (long) Math.ceil(minutesAfterGrace / 60.0);
    }

    @Override
    public long calculateLateDays(Rental rental, LocalDateTime currentTime) {
        long lateHours = calculateLateHours(rental, currentTime);

        return (long) Math.ceil(lateHours / 24.0);
    }
}
