package com.akif.rental.internal.service;

import com.akif.shared.time.UtcDates;
import java.time.Clock;
import com.akif.payment.api.PaymentDto;
import com.akif.rental.internal.dto.penalty.PenaltyResult;
import com.akif.rental.api.PenaltySummaryEvent;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.service.penalty.PenaltyCalculationService;
import com.akif.rental.internal.service.penalty.PenaltyPaymentService;
import com.akif.payment.api.PaymentService;
import com.akif.payment.api.PaymentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.util.Optional;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class RentalLatePenaltyProcessor {

    private final Clock clock;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher eventPublisher;
    private final PenaltyCalculationService penaltyCalculationService;
    private final PenaltyPaymentService penaltyPaymentService;

    void processPenaltyForLateReturn(Rental rental, Instant actualReturnTime) {
        log.debug("Processing penalty for late rental: {}", rental.getId());

        try {
            PenaltyResult penaltyResult = penaltyCalculationService.calculatePenalty(
                    rental, actualReturnTime);

            log.info("Calculated penalty for rental {}: Amount={} {}, Late Hours={}, Late Days={}",
                    rental.getId(),
                    penaltyResult.penaltyAmount(),
                    rental.getCurrency(),
                    penaltyResult.lateHours(),
                    penaltyResult.lateDays());

            rental.flagLate(penaltyResult.status(), actualReturnTime, penaltyResult.lateHours());
            rental.assessPenalty(penaltyResult.penaltyAmount(), penaltyResult.lateHours());

            if (penaltyResult.penaltyAmount() == null ||
                    penaltyResult.penaltyAmount().compareTo(BigDecimal.ZERO) <= 0) {
                log.info("No penalty to charge for rental {} (within grace period or no late hours)",
                        rental.getId());
                rental.recordPenaltyPayment(false);
                return;
            }

            Optional<PaymentDto> alreadyCollected = paymentService.getPenaltyPaymentByRentalId(rental.getId());
            if (alreadyCollected.isPresent()) {
                rental.assessPenalty(alreadyCollected.get().amount(), penaltyResult.lateHours());
                rental.recordPenaltyPayment(true);
                log.warn("Penalty of rental {} was already collected, no second charge is made", rental.getId());
                return;
            }

            PaymentDto penaltyPaymentDto = penaltyPaymentService.createPenaltyPayment(
                    rental, penaltyResult.penaltyAmount());

            PaymentResult chargeResult = penaltyPaymentService.chargePenalty(
                    penaltyPaymentDto.id(), rental.getUserId());

            if (chargeResult.success()) {
                rental.recordPenaltyPayment(true);
                log.info("Successfully charged penalty for rental: {}", rental.getId());
            } else {
                rental.recordPenaltyPayment(false);
                if (!chargeResult.outcomeUnknown()) {
                    penaltyPaymentService.handleFailedPenaltyPayment(penaltyPaymentDto, chargeResult.message());
                }
            }

            publishPenaltySummaryEvent(rental, penaltyResult, actualReturnTime);

        } catch (Exception e) {
            log.error("Error processing penalty for rental {}: {}",
                    rental.getId(), e.getMessage(), e);
            rental.recordPenaltyPayment(false);
        }
    }

    private void publishPenaltySummaryEvent(Rental rental, PenaltyResult penaltyResult,
            Instant actualReturnTime) {
        Instant scheduledReturnTime = UtcDates.endOfDayWholeSecond(rental.getEndDate());

        PenaltySummaryEvent event = new PenaltySummaryEvent(
                rental.getId(),
                rental.getUserEmail(),
                Instant.now(clock),
                rental.getCarBrand(),
                rental.getCarModel(),
                rental.getCarLicensePlate(),
                scheduledReturnTime,
                actualReturnTime,
                penaltyResult.lateHours(),
                penaltyResult.lateDays(),
                penaltyResult.penaltyAmount(),
                rental.getCurrency(),
                penaltyResult.breakdown(),
                penaltyResult.cappedAtMax());

        eventPublisher.publishEvent(event);
        log.info("Published PenaltySummaryEvent for rental: {}", rental.getId());
    }
}
