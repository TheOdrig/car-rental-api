package com.akif.scheduler;

import com.akif.enums.RentalStatus;
import com.akif.event.PickupReminderEvent;
import com.akif.event.ReturnReminderEvent;
import com.akif.model.Rental;
import com.akif.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final RentalRepository rentalRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void sendPickupReminders() {
        log.info("Scheduled pickup reminder job started");
        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            int pageNumber = 0;
            int pageSize = 50;
            Page<Rental> rentalsPage;

            do {
                Pageable pageable = PageRequest.of(pageNumber, pageSize);
                rentalsPage = rentalRepository.findRentalsForPickupReminder(
                        tomorrow,
                        RentalStatus.CONFIRMED,
                        pageable
                );

                for (Rental rental : rentalsPage.getContent()) {
                    try {
                        publishPickupReminderEvent(rental);
                        rental.setPickupReminderSent(true);
                        rentalRepository.save(rental);
                        log.info("Pickup reminder sent for rental ID: {}", rental.getId());
                    } catch (Exception e) {
                        log.error("Failed to send pickup reminder for rental ID: {}. Error: {}",
                                rental.getId(), e.getMessage(), e);
                    }
                }

                pageNumber++;
            } while (rentalsPage.hasNext());

            log.info("Scheduled pickup reminder job completed. Total reminders sent: {}",
                    rentalsPage.getTotalElements());
        } catch (Exception e) {
            log.error("Scheduled pickup reminder job failed: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendReturnReminders() {
        log.info("Scheduled return reminder job started");
        try {
            LocalDate today = LocalDate.now();
            int pageNumber = 0;
            int pageSize = 50;
            Page<Rental> rentalsPage;

            do {
                Pageable pageable = PageRequest.of(pageNumber, pageSize);
                rentalsPage = rentalRepository.findRentalsForReturnReminder(
                        today,
                        RentalStatus.IN_USE,
                        pageable
                );

                for (Rental rental : rentalsPage.getContent()) {
                    try {
                        publishReturnReminderEvent(rental);
                        rental.setReturnReminderSent(true);
                        rentalRepository.save(rental);
                        log.info("Return reminder sent for rental ID: {}", rental.getId());
                    } catch (Exception e) {
                        log.error("Failed to send return reminder for rental ID: {}. Error: {}",
                                rental.getId(), e.getMessage(), e);
                    }
                }

                pageNumber++;
            } while (rentalsPage.hasNext());

            log.info("Scheduled return reminder job completed. Total reminders sent: {}",
                    rentalsPage.getTotalElements());
        } catch (Exception e) {
            log.error("Scheduled return reminder job failed: {}", e.getMessage(), e);
        }
    }

    private void publishPickupReminderEvent(Rental rental) {
        PickupReminderEvent event = new PickupReminderEvent(
                this,
                rental.getId(),
                rental.getUser().getEmail(),
                LocalDateTime.now(),
                rental.getStartDate(),
                "Pickup Location", // TODO: Add pickup location to Rental entity if needed
                rental.getCar().getBrand(),
                rental.getCar().getModel()
        );
        eventPublisher.publishEvent(event);
    }

    private void publishReturnReminderEvent(Rental rental) {
        ReturnReminderEvent event = new ReturnReminderEvent(
                this,
                rental.getId(),
                rental.getUser().getEmail(),
                LocalDateTime.now(),
                rental.getEndDate(),
                "Return Location", // TODO: Add return location to Rental entity if needed
                rental.getDailyPrice()
        );
        eventPublisher.publishEvent(event);
    }
}
