package com.akif.rental.internal.service.detection.impl;

import java.time.Clock;

import com.akif.rental.internal.config.PenaltyConfig;
import com.akif.rental.internal.dto.penalty.PenaltyResult;
import com.akif.rental.domain.enums.LateReturnStatus;
import com.akif.rental.api.GracePeriodWarningEvent;
import com.akif.rental.api.LateReturnNotificationEvent;
import com.akif.rental.api.SeverelyLateNotificationEvent;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.rental.internal.service.detection.LateReturnDetectionService;
import com.akif.rental.internal.service.penalty.PenaltyCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
public class LateReturnDetectionServiceImpl implements LateReturnDetectionService {

    private final Clock clock;

    private final RentalRepository rentalRepository;
    private final PenaltyConfig penaltyConfig;
    private final PenaltyCalculationService penaltyCalculationService;
    private final ApplicationEventPublisher eventPublisher;

    private static final int PAGE_SIZE = 50;

    @Override
    @Transactional
    public void detectLateReturns() {
        log.info("Starting late return detection process");
        
        LocalDate currentDate = LocalDate.now(clock);
        LocalDateTime currentTime = LocalDateTime.now(clock);
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
                        LateReturnStatus oldStatus = rental.getLateReturnStatus();
                        LateReturnStatus newStatus = calculateLateStatus(rental, currentTime);

                        if (oldStatus != newStatus) {
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

                            publishEventForStatusChange(rental, newStatus, currentTime);
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
    
    private void publishEventForStatusChange(Rental rental, LateReturnStatus newStatus, LocalDateTime currentTime) {
        try {
            String carBrand = rental.getCarBrand();
            String carModel = rental.getCarModel();
            String licensePlate = rental.getCarLicensePlate();
            LocalDateTime scheduledReturnTime = rental.getEndDate().atTime(LocalTime.MAX);
            
            switch (newStatus) {
                case GRACE_PERIOD:
                    publishGracePeriodWarning(rental, currentTime, carBrand, carModel, licensePlate, scheduledReturnTime);
                    break;
                    
                case LATE:
                    publishLateReturnNotification(rental, currentTime, carBrand, carModel, licensePlate, scheduledReturnTime);
                    break;
                    
                case SEVERELY_LATE:
                    publishSeverelyLateNotification(rental, currentTime, carBrand, carModel, licensePlate, scheduledReturnTime);
                    break;
                    
                default:
                    break;
            }
        } catch (Exception e) {
            log.error("Error publishing event for rental {}: {}", rental.getId(), e.getMessage(), e);
        }
    }
    
    private void publishGracePeriodWarning(Rental rental, LocalDateTime currentTime, 
                                          String carBrand, String carModel, String licensePlate,
                                          LocalDateTime scheduledReturnTime) {
        long minutesLate = Duration.between(scheduledReturnTime, currentTime).toMinutes();
        int remainingGraceMinutes = (int) (penaltyConfig.getGracePeriodMinutes() - minutesLate);
        
        if (remainingGraceMinutes < 0) {
            remainingGraceMinutes = 0;
        }

        GracePeriodWarningEvent event = new GracePeriodWarningEvent(
                rental.getId(),
                rental.getUserEmail(),
                currentTime,
                carBrand,
                carModel,
                licensePlate,
                scheduledReturnTime,
                remainingGraceMinutes
        );
        
        eventPublisher.publishEvent(event);
        log.info("Published GracePeriodWarningEvent for rental: {}", rental.getId());
    }
    
    private void publishLateReturnNotification(Rental rental, LocalDateTime currentTime,
                                              String carBrand, String carModel, String licensePlate,
                                              LocalDateTime scheduledReturnTime) {
        int lateHours = rental.getLateHours() != null ? rental.getLateHours() : 0;
        
        PenaltyResult penaltyResult = penaltyCalculationService.calculatePenalty(rental, currentTime);

        LateReturnNotificationEvent event = new LateReturnNotificationEvent(
                rental.getId(),
                rental.getUserEmail(),
                currentTime,
                carBrand,
                carModel,
                licensePlate,
                scheduledReturnTime,
                lateHours,
                penaltyResult.penaltyAmount(),
                rental.getCurrency()
        );
        
        eventPublisher.publishEvent(event);
        log.info("Published LateReturnNotificationEvent for rental: {}", rental.getId());
    }
    
    private void publishSeverelyLateNotification(Rental rental, LocalDateTime currentTime,
                                                String carBrand, String carModel, String licensePlate,
                                                LocalDateTime scheduledReturnTime) {
        int lateHours = rental.getLateHours() != null ? rental.getLateHours() : 0;
        int lateDays = (int) calculateLateDays(rental, currentTime);
        
        PenaltyResult penaltyResult = penaltyCalculationService.calculatePenalty(rental, currentTime);
        
        String escalationWarning = "Your rental is severely overdue. Please return the vehicle immediately " +
                "to avoid further penalties and potential legal action.";

        SeverelyLateNotificationEvent event = new SeverelyLateNotificationEvent(
                rental.getId(),
                rental.getUserEmail(),
                currentTime,
                carBrand,
                carModel,
                licensePlate,
                scheduledReturnTime,
                lateHours,
                lateDays,
                penaltyResult.penaltyAmount(),
                rental.getCurrency(),
                escalationWarning
        );
        
        eventPublisher.publishEvent(event);
        log.info("Published SeverelyLateNotificationEvent for rental: {}", rental.getId());
    }
}
