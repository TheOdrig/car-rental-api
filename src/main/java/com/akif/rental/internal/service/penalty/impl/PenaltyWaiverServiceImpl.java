package com.akif.rental.internal.service.penalty.impl;

import java.time.Clock;

import com.akif.payment.api.PaymentDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public PenaltyWaiver waivePenalty(Long rentalId, BigDecimal waiverAmount, String reason, Long adminId) {
        log.debug("Waiving penalty for rental: {}, amount: {}", rentalId, waiverAmount);

        validateWaiverRequest(rentalId, waiverAmount, reason, adminId);

        Rental rental = rentalRepository.findByIdForUpdate(rentalId)
                .orElseThrow(() -> new RentalNotFoundException("Rental not found with id: " + rentalId));

        if (rental.getPenaltyAmount() == null || rental.getPenaltyAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PenaltyWaiverException("Rental has no penalty to waive");
        }

        if (waiverAmount.compareTo(rental.getPenaltyAmount()) > 0) {
            throw new PenaltyWaiverException(
                    String.format("Waiver amount (%.2f) cannot exceed penalty amount (%.2f)",
                            waiverAmount, rental.getPenaltyAmount())
            );
        }

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

        paymentService.closeOutstandingPenaltyPayments(rentalId);
        boolean collected = paymentService.getPenaltyPaymentByRentalId(rentalId).isPresent();

        rental.reducePenaltyTo(remainingPenalty);
        rental.recordPenaltyPayment(collected && remainingPenalty.signum() > 0);
        rentalRepository.save(rental);

        if (collected) {
            processRefundForWaiver(savedWaiver);
        } else if (remainingPenalty.signum() > 0) {
            collectRemainder(rental, remainingPenalty);
        }

        log.info("Successfully waived penalty for rental: {}, waived amount: {}, remaining: {}",
                rentalId, waiverAmount, remainingPenalty);

        return savedWaiver;
    }

    private void collectRemainder(Rental rental, BigDecimal remainingPenalty) {
        PaymentDto remainder = null;
        try {
            remainder = penaltyPaymentService.createPenaltyPayment(rental, remainingPenalty);
            PaymentResult result = penaltyPaymentService.chargePenalty(remainder.id(), rental.getUserId());
            if (result.success()) {
                rental.recordPenaltyPayment(true);
                rentalRepository.save(rental);
            } else if (!result.outcomeUnknown()) {
                penaltyPaymentService.handleFailedPenaltyPayment(remainder, result.message());
            }
        } catch (RuntimeException e) {
            log.error("Could not collect the remaining penalty of rental {}: {}", rental.getId(), e.getMessage());
            if (remainder != null) {
                penaltyPaymentService.handleFailedPenaltyPayment(remainder, e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public PenaltyWaiver waiveFullPenalty(Long rentalId, String reason, Long adminId) {
        log.debug("Waiving full penalty for rental: {}", rentalId);

        Rental rental = rentalRepository.findByIdForUpdate(rentalId)
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

