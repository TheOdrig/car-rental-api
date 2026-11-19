package com.akif.rental.internal.service.penalty.impl;

import java.time.Clock;

import com.akif.payment.api.PaymentAttentionEvent;
import com.akif.payment.api.PaymentDto;
import com.akif.rental.api.PenaltyChargeFailedEvent;
import com.akif.rental.internal.exception.PenaltyWaiverException;
import com.akif.rental.internal.exception.RentalNotFoundException;
import com.akif.rental.domain.model.PenaltyWaiver;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.repository.PenaltyWaiverRepository;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.payment.api.PaymentService;
import com.akif.payment.api.PaymentResult;
import com.akif.rental.internal.service.penalty.PenaltyPaymentService;
import com.akif.rental.internal.service.penalty.PenaltyWaiverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PenaltyWaiverServiceImpl implements PenaltyWaiverService {

    private final Clock clock;

    private final RentalRepository rentalRepository;
    private final PenaltyWaiverRepository penaltyWaiverRepository;
    private final PaymentService paymentService;
    private final PenaltyPaymentService penaltyPaymentService;
    private final PlatformTransactionManager transactionManager;
    private final ApplicationEventPublisher eventPublisher;

    private record Recorded(PenaltyWaiver waiver, boolean collected, BigDecimal remainingPenalty) {
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PenaltyWaiver waivePenalty(Long rentalId, BigDecimal waiverAmount, String reason, Long adminId) {
        log.debug("Waiving penalty for rental: {}, amount: {}", rentalId, waiverAmount);

        validateWaiverRequest(rentalId, waiverAmount, reason, adminId);
        Rental current = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new RentalNotFoundException("Rental not found with id: " + rentalId));
        validateAgainstPenalty(current, waiverAmount);

        paymentService.closeOutstandingPenaltyPayments(rentalId);

        Recorded recorded = new TransactionTemplate(transactionManager).execute(
                status -> recordWaiver(rentalId, waiverAmount, reason, adminId));
        PenaltyWaiver waiver = recorded.waiver();

        if (recorded.collected()) {
            refundAfterWaiver(waiver);
        } else if (recorded.remainingPenalty().signum() > 0) {
            collectRemainder(rentalId, recorded.remainingPenalty());
        }

        log.info("Successfully waived penalty for rental: {}, waived amount: {}, remaining: {}",
                rentalId, waiverAmount, recorded.remainingPenalty());

        return waiver;
    }

    private Recorded recordWaiver(Long rentalId, BigDecimal waiverAmount, String reason, Long adminId) {
        Rental rental = rentalRepository.findByIdForUpdate(rentalId)
                .orElseThrow(() -> new RentalNotFoundException("Rental not found with id: " + rentalId));
        validateAgainstPenalty(rental, waiverAmount);

        BigDecimal remainingPenalty = rental.getPenaltyAmount().subtract(waiverAmount);

        PenaltyWaiver waiver = PenaltyWaiver.builder()
                .rental(rental)
                .originalPenalty(rental.getPenaltyAmount())
                .waivedAmount(waiverAmount)
                .remainingPenalty(remainingPenalty)
                .reason(reason)
                .adminId(adminId)
                .waivedAt(LocalDateTime.now(clock))
                .refundInitiated(false)
                .isDeleted(false)
                .build();

        PenaltyWaiver savedWaiver = penaltyWaiverRepository.save(waiver);

        boolean collected = paymentService.getPenaltyPaymentByRentalId(rentalId).isPresent();

        rental.reducePenaltyTo(remainingPenalty);
        rental.recordPenaltyPayment(collected && remainingPenalty.signum() > 0);
        rentalRepository.save(rental);

        return new Recorded(savedWaiver, collected, remainingPenalty);
    }

    private void validateAgainstPenalty(Rental rental, BigDecimal waiverAmount) {
        if (rental.getPenaltyAmount() == null || rental.getPenaltyAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PenaltyWaiverException("Rental has no penalty to waive");
        }

        if (waiverAmount.compareTo(rental.getPenaltyAmount()) > 0) {
            throw new PenaltyWaiverException(
                    String.format("Waiver amount (%.2f) cannot exceed penalty amount (%.2f)",
                            waiverAmount, rental.getPenaltyAmount())
            );
        }
    }

    private void refundAfterWaiver(PenaltyWaiver waiver) {
        try {
            processRefundForWaiver(waiver);
        } catch (RuntimeException e) {
            Long rentalId = waiver.getRental().getId();
            log.error("The waiver {} of rental {} was recorded but its refund failed: {}",
                    waiver.getId(), rentalId, e.getMessage());
            Optional<PaymentDto> payment = paymentService.getPenaltyPaymentByRentalId(rentalId);
            new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                    eventPublisher.publishEvent(new PaymentAttentionEvent(
                            payment.map(PaymentDto::id).orElse(null),
                            rentalId,
                            PaymentAttentionEvent.Reason.AUTO_REFUND_FAILED,
                            waiver.getWaivedAmount(),
                            waiver.getRental().getCurrency(),
                            LocalDateTime.now(clock))));
        }
    }

    private void collectRemainder(Long rentalId, BigDecimal remainingPenalty) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new RentalNotFoundException("Rental not found with id: " + rentalId));
        PaymentDto remainder = null;
        try {
            remainder = penaltyPaymentService.createPenaltyPayment(rental, remainingPenalty);
            PaymentResult result = penaltyPaymentService.chargePenalty(remainder.id(), rental.getUserId());
            if (result.success()) {
                markPenaltyPaid(rentalId);
            } else if (!result.outcomeUnknown()) {
                penaltyPaymentService.handleFailedPenaltyPayment(remainder, result.message());
            }
        } catch (RuntimeException e) {
            log.error("Could not collect the remaining penalty of rental {}: {}", rentalId, e.getMessage());
            reportRemainderFailure(rental, remainingPenalty, remainder, e.getMessage());
        }
    }

    private void markPenaltyPaid(Long rentalId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                rentalRepository.findByIdForUpdate(rentalId).ifPresent(locked -> {
                    locked.recordPenaltyPayment(true);
                    rentalRepository.save(locked);
                }));
    }

    private void reportRemainderFailure(Rental rental, BigDecimal remainingPenalty, PaymentDto remainder, String reason) {
        try {
            if (remainder != null) {
                penaltyPaymentService.handleFailedPenaltyPayment(remainder, reason);
                return;
            }
            new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                    eventPublisher.publishEvent(new PenaltyChargeFailedEvent(
                            rental.getId(),
                            null,
                            rental.getUserEmail(),
                            LocalDateTime.now(clock),
                            remainingPenalty,
                            rental.getCurrency(),
                            reason)));
        } catch (RuntimeException e) {
            log.error("Could not raise the alert for the uncollected penalty of rental {}: {}",
                    rental.getId(), e.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PenaltyWaiver waiveFullPenalty(Long rentalId, String reason, Long adminId) {
        log.debug("Waiving full penalty for rental: {}", rentalId);

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new RentalNotFoundException("Rental not found with id: " + rentalId));

        if (rental.getPenaltyAmount() == null || rental.getPenaltyAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PenaltyWaiverException("Rental has no penalty to waive");
        }

        return waivePenalty(rentalId, rental.getPenaltyAmount(), reason, adminId);
    }


    @Override
    public List<PenaltyWaiver> getPenaltyHistory(Long rentalId) {
        log.debug("Getting penalty history for rental: {}", rentalId);

        if (!rentalRepository.existsById(rentalId)) {
            throw new RentalNotFoundException("Rental not found with id: " + rentalId);
        }

        return penaltyWaiverRepository.findByRentalIdAndIsDeletedFalse(rentalId);
    }

    @Override
    @Transactional
    public void processRefundForWaiver(PenaltyWaiver waiver) {
        log.debug("Processing refund for waiver: {}", waiver.getId());

        if (Boolean.TRUE.equals(waiver.getRefundInitiated())) {
            log.warn("Refund already initiated for waiver: {}", waiver.getId());
            return;
        }

        Long rentalId = waiver.getRental().getId();
        Optional<PaymentDto> paymentOpt = paymentService.getPenaltyPaymentByRentalId(rentalId);

        if (paymentOpt.isEmpty()) {
            log.warn("No payment found for rental: {}, cannot process refund", rentalId);
            return;
        }

        PaymentDto payment = paymentOpt.get();

        if (!payment.canRefund()) {
            log.warn("Payment {} cannot be refunded, status: {}", payment.id(), payment.status());
            return;
        }

        if (payment.transactionId() == null || payment.transactionId().isEmpty()) {
            log.warn("Payment {} has no transaction ID, cannot process refund", payment.id());
            return;
        }

        try {
            PaymentResult refundResult = paymentService.refundPayment(
                    payment.id(), waiver.getWaivedAmount(), refundOperationKey(waiver, rentalId));

            if (refundResult.success()) {
                waiver.recordRefund(refundResult.transactionId());
                penaltyWaiverRepository.save(waiver);

                log.info("Successfully processed refund for waiver: {}, amount: {}, transaction: {}",
                        waiver.getId(), waiver.getWaivedAmount(), refundResult.transactionId());
            } else {
                log.error("Refund failed for waiver: {}, reason: {}", 
                        waiver.getId(), refundResult.message());
                throw new PenaltyWaiverException("Refund failed: " + refundResult.message());
            }
        } catch (PenaltyWaiverException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing refund for waiver: {}", waiver.getId(), e);
            throw new PenaltyWaiverException("Failed to process refund", e);
        }
    }

    private String refundOperationKey(PenaltyWaiver waiver, Long rentalId) {
        BigDecimal earlier = penaltyWaiverRepository.findByRentalIdAndIsDeletedFalse(rentalId).stream()
                .filter(w -> w.getId() != null && waiver.getId() != null && w.getId() < waiver.getId())
                .map(PenaltyWaiver::getWaivedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cumulative = earlier.add(waiver.getWaivedAmount()).setScale(2, RoundingMode.HALF_UP);
        return "rental-" + rentalId + ":waived-" + cumulative.toPlainString() + ":refund";
    }

    private void validateWaiverRequest(Long rentalId, BigDecimal waiverAmount, String reason, Long adminId) {
        if (rentalId == null) {
            throw new PenaltyWaiverException("Rental ID cannot be null");
        }

        if (waiverAmount == null || waiverAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PenaltyWaiverException("Waiver amount must be positive");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new PenaltyWaiverException("Waiver reason is mandatory");
        }

        if (adminId == null) {
            throw new PenaltyWaiverException("Admin ID cannot be null");
        }
    }
}

