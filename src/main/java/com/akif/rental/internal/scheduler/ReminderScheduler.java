package com.akif.rental.internal.scheduler;

import java.time.Clock;

import com.akif.rental.api.RentalStatus;
import com.akif.rental.api.PickupReminderEvent;
import com.akif.rental.api.ReturnReminderEvent;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.config.PenaltyConfig;
import com.akif.rental.internal.config.ReminderProperties;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.shared.enums.CurrencyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final Clock clock;

    private final RentalRepository rentalRepository;
    private final PenaltyConfig penaltyConfig;
    private final ApplicationEventPublisher eventPublisher;
    private final ReminderProperties properties;

    @Scheduled(cron = "${rental.reminder.pickup-cron:0 0 8 * * *}")
    @Transactional
    public void sendPickupReminders() {
        log.info("Scheduled pickup reminder job started");
        try {
            LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
            int sent = remindInBatches(
                    afterId -> rentalRepository.findRentalsForPickupReminder(
                            tomorrow, RentalStatus.CONFIRMED, afterId, PageRequest.of(0, properties.batchSize())),
                    rental -> {
                        publishPickupReminderEvent(rental);
                        rental.markPickupReminderSent();
                    });
            log.info("Scheduled pickup reminder job completed. Total reminders sent: {}", sent);
        } catch (Exception e) {
            log.error("Scheduled pickup reminder job failed: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "${rental.reminder.return-cron:0 0 9 * * *}")
    @Transactional
    public void sendReturnReminders() {
        log.info("Scheduled return reminder job started");
        try {
            LocalDate today = LocalDate.now(clock);
            int sent = remindInBatches(
                    afterId -> rentalRepository.findRentalsForReturnReminder(
                            today, RentalStatus.IN_USE, afterId, PageRequest.of(0, properties.batchSize())),
                    rental -> {
                        publishReturnReminderEvent(rental);
                        rental.markReturnReminderSent();
                    });
            log.info("Scheduled return reminder job completed. Total reminders sent: {}", sent);
        } catch (Exception e) {
            log.error("Scheduled return reminder job failed: {}", e.getMessage(), e);
        }
    }

    private int remindInBatches(Function<Long, List<Rental>> nextBatch, Consumer<Rental> remind) {
        long lastId = 0L;
        int sent = 0;
        for (int batch = 0; batch < properties.maxBatches(); batch++) {
            List<Rental> rentals = nextBatch.apply(lastId);
            if (rentals.isEmpty()) {
                return sent;
            }
            for (Rental rental : rentals) {
                lastId = Math.max(lastId, rental.getId());
                try {
                    remind.accept(rental);
                    rentalRepository.save(rental);
                    sent++;
                    log.info("Reminder sent for rental ID: {}", rental.getId());
                } catch (Exception e) {
                    log.error("Failed to send reminder for rental ID: {}. Error: {}",
                            rental.getId(), e.getMessage(), e);
                }
            }
            if (rentals.size() < properties.batchSize()) {
                return sent;
            }
        }
        log.warn("Reminder job stopped after {} batches", properties.maxBatches());
        return sent;
    }

    private void publishPickupReminderEvent(Rental rental) {
        PickupReminderEvent event = new PickupReminderEvent(
                rental.getId(),
                rental.getUserEmail(),
                Instant.now(clock),
                rental.getStartDate(),
                rental.getCarBrand(),
                rental.getCarModel()
        );
        eventPublisher.publishEvent(event);
    }

    private void publishReturnReminderEvent(Rental rental) {
        CurrencyType currency = rental.getCurrency();
        ReturnReminderEvent event = new ReturnReminderEvent(
                rental.getId(),
                rental.getUserEmail(),
                Instant.now(clock),
                rental.getEndDate(),
                currency,
                rental.getDailyPrice().multiply(penaltyConfig.hourlyPenaltyRate())
                        .setScale(currency.getDecimalPlaces(), RoundingMode.HALF_UP),
                rental.getDailyPrice().multiply(penaltyConfig.dailyPenaltyRate())
                        .setScale(currency.getDecimalPlaces(), RoundingMode.HALF_UP),
                penaltyConfig.gracePeriodMinutes());
        eventPublisher.publishEvent(event);
    }
}
