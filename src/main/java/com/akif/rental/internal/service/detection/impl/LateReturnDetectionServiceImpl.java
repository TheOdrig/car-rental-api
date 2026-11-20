package com.akif.rental.internal.service.detection.impl;

import com.akif.shared.time.LateHours;
import com.akif.shared.time.RentalDays;
import com.akif.shared.time.UtcDates;
import java.time.Clock;

import com.akif.rental.internal.config.PenaltyConfig;
import com.akif.rental.internal.dto.penalty.PenaltyResult;
import com.akif.rental.domain.enums.LateReturnStatus;
import com.akif.rental.api.GracePeriodWarningEvent;
import com.akif.rental.api.LateReturnNotificationEvent;
import com.akif.rental.api.SeverelyLateNotificationEvent;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.rental.internal.service.detection.LateReturnDetectionService;
import com.akif.rental.internal.service.penalty.PenaltyCalculationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
public class LateReturnDetectionServiceImpl implements LateReturnDetectionService {

    private final Clock clock;

    private final RentalRepository rentalRepository;
    private final PenaltyConfig penaltyConfig;
    private final PenaltyCalculationService penaltyCalculationService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public LateReturnDetectionServiceImpl(
            Clock clock,
            RentalRepository rentalRepository,
            PenaltyConfig penaltyConfig,
            PenaltyCalculationService penaltyCalculationService,
            ApplicationEventPublisher eventPublisher,
            PlatformTransactionManager transactionManager) {
        this.clock = clock;
        this.rentalRepository = rentalRepository;
        this.penaltyConfig = penaltyConfig;
        this.penaltyCalculationService = penaltyCalculationService;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void detectLateReturns() {
        log.info("Starting late return detection process");

        LocalDate currentDate = LocalDate.now(clock);
        Instant currentTime = Instant.now(clock);
        List<Long> overdueIds = rentalRepository.findOverdueRentalIds(currentDate);
        int totalUpdated = 0;

        for (Long rentalId : overdueIds) {
            try {
                Boolean updated = transactionTemplate.execute(status -> flagIfStatusChanged(rentalId, currentDate, currentTime));
                if (Boolean.TRUE.equals(updated)) {
                    totalUpdated++;
                }
            } catch (RuntimeException e) {
                log.error("Error processing rental {}: {}", rentalId, e.getMessage(), e);
            }
        }

        log.info("Late return detection completed. Processed: {}, Updated: {}", overdueIds.size(), totalUpdated);
    }

    private boolean flagIfStatusChanged(Long rentalId, LocalDate currentDate, Instant currentTime) {
        Rental rental = rentalRepository.findByIdForUpdate(rentalId).orElse(null);
        if (rental == null || rental.getStatus() != RentalStatus.IN_USE || !rental.getEndDate().isBefore(currentDate)) {
            return false;
        }
        LateReturnStatus oldStatus = rental.getLateReturnStatus();
        LateReturnStatus newStatus = calculateLateStatus(rental, currentTime);
        if (oldStatus == newStatus) {
            return false;
        }
        long lateHours = calculateLateHours(rental, currentTime);
        rental.flagLate(newStatus, currentTime, (int) lateHours);
        rentalRepository.save(rental);

        log.debug("Updated rental {} to status: {}, late hours: {}", rental.getId(), newStatus, lateHours);

        publishEventForStatusChange(rental, newStatus, currentTime);
        return true;
    }
    @Override
    public LateReturnStatus calculateLateStatus(Rental rental, Instant currentTime) {
        if (rental == null || rental.getEndDate() == null) {
            throw new IllegalArgumentException("Rental and end date cannot be null");
        }

        Instant rentalEndDateTime = UtcDates.endOfDayWholeSecond(rental.getEndDate());

        if (!currentTime.isAfter(rentalEndDateTime)) {
            return LateReturnStatus.ON_TIME;
        }

        long minutesLate = Duration.between(rentalEndDateTime, currentTime).toMinutes();

        if (minutesLate <= penaltyConfig.gracePeriodMinutes()) {
            return LateReturnStatus.GRACE_PERIOD;
        }

        long hoursLate = calculateLateHours(rental, currentTime);

        if (hoursLate >= penaltyConfig.severelyLateThresholdHours()) {
            return LateReturnStatus.SEVERELY_LATE;
        }

        return LateReturnStatus.LATE;
    }

    @Override
    public long calculateLateHours(Rental rental, Instant currentTime) {
        if (rental == null || rental.getEndDate() == null) {
            return 0;
        }

        return LateHours.between(rental.getEndDate(), currentTime, penaltyConfig.gracePeriodMinutes());
    }

    @Override
    public long calculateLateDays(Rental rental, Instant currentTime) {
        long lateHours = calculateLateHours(rental, currentTime);

        return RentalDays.lateDays(lateHours);
    }
    
    private void publishEventForStatusChange(Rental rental, LateReturnStatus newStatus, Instant currentTime) {
        try {
            String carBrand = rental.getCarBrand();
            String carModel = rental.getCarModel();
            String licensePlate = rental.getCarLicensePlate();
            Instant scheduledReturnTime = UtcDates.endOfDayWholeSecond(rental.getEndDate());
            
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
    
    private void publishGracePeriodWarning(Rental rental, Instant currentTime, 
                                          String carBrand, String carModel, String licensePlate,
                                          Instant scheduledReturnTime) {
        long minutesLate = Duration.between(scheduledReturnTime, currentTime).toMinutes();
        int remainingGraceMinutes = (int) (penaltyConfig.gracePeriodMinutes() - minutesLate);
        
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
    
    private void publishLateReturnNotification(Rental rental, Instant currentTime,
                                              String carBrand, String carModel, String licensePlate,
                                              Instant scheduledReturnTime) {
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
    
    private void publishSeverelyLateNotification(Rental rental, Instant currentTime,
                                                String carBrand, String carModel, String licensePlate,
                                                Instant scheduledReturnTime) {
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
