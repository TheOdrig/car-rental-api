package com.akif.service.penalty.impl;

import com.akif.enums.PaymentStatus;
import com.akif.exception.PenaltyWaiverException;
import com.akif.exception.RentalNotFoundException;
import com.akif.model.Payment;
import com.akif.model.PenaltyWaiver;
import com.akif.model.Rental;
import com.akif.repository.PaymentRepository;
import com.akif.repository.PenaltyWaiverRepository;
import com.akif.repository.RentalRepository;
import com.akif.service.gateway.IPaymentGateway;
import com.akif.service.gateway.PaymentResult;
import com.akif.service.penalty.IPenaltyWaiverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PenaltyWaiverServiceImpl implements IPenaltyWaiverService {

    private final RentalRepository rentalRepository;
    private final PenaltyWaiverRepository penaltyWaiverRepository;
    private final PaymentRepository paymentRepository;
    private final IPaymentGateway paymentGateway;

    @Override
    @Transactional
    public PenaltyWaiver waivePenalty(Long rentalId, BigDecimal waiverAmount, String reason, Long adminId) {
        log.debug("Waiving penalty for rental: {}, amount: {}", rentalId, waiverAmount);

        validateWaiverRequest(rentalId, waiverAmount, reason, adminId);

        Rental rental = rentalRepository.findByIdAndIsDeletedFalse(rentalId)
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
                .waivedAt(LocalDateTime.now())
                .refundInitiated(false)
                .isDeleted(false)
                .build();

        PenaltyWaiver savedWaiver = penaltyWaiverRepository.save(waiver);

        rental.setPenaltyAmount(remainingPenalty);
        rentalRepository.save(rental);

        if (Boolean.TRUE.equals(rental.getPenaltyPaid())) {
            processRefundForWaiver(savedWaiver);
        }

        log.info("Successfully waived penalty for rental: {}, waived amount: {}, remaining: {}",
                rentalId, waiverAmount, remainingPenalty);

        return savedWaiver;
    }

    @Override
    @Transactional
    public PenaltyWaiver waiveFullPenalty(Long rentalId, String reason, Long adminId) {
        log.debug("Waiving full penalty for rental: {}", rentalId);

        Rental rental = rentalRepository.findByIdAndIsDeletedFalse(rentalId)
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

        Payment penaltyPayment = paymentRepository.findByRentalIdAndIsDeletedFalse(waiver.getRental().getId())
                .orElse(null);

        if (penaltyPayment == null) {
            log.warn("No payment found for rental: {}, cannot process refund", waiver.getRental().getId());
            return;
        }

        if (!penaltyPayment.canRefund()) {
            log.warn("Payment {} cannot be refunded, status: {}", 
                    penaltyPayment.getId(), penaltyPayment.getStatus());
            return;
        }

        if (penaltyPayment.getTransactionId() == null || penaltyPayment.getTransactionId().isEmpty()) {
            log.warn("Payment {} has no transaction ID, cannot process refund", penaltyPayment.getId());
            return;
        }

        try {
            PaymentResult refundResult = paymentGateway.refund(
                    penaltyPayment.getTransactionId(),
                    waiver.getWaivedAmount()
            );

            if (refundResult.success()) {
                waiver.setRefundInitiated(true);
                waiver.setRefundTransactionId(refundResult.transactionId());
                penaltyWaiverRepository.save(waiver);

                BigDecimal currentRefunded = penaltyPayment.getRefundedAmount() != null 
                        ? penaltyPayment.getRefundedAmount() 
                        : BigDecimal.ZERO;
                penaltyPayment.setRefundedAmount(currentRefunded.add(waiver.getWaivedAmount()));

                if (penaltyPayment.getRefundedAmount().compareTo(penaltyPayment.getAmount()) >= 0) {
                    penaltyPayment.updateStatus(PaymentStatus.REFUNDED);
                }
                
                paymentRepository.save(penaltyPayment);

                log.info("Successfully processed refund for waiver: {}, amount: {}, transaction: {}",
                        waiver.getId(), waiver.getWaivedAmount(), refundResult.transactionId());
            } else {
                log.error("Refund failed for waiver: {}, reason: {}", 
                        waiver.getId(), refundResult.message());
                throw new PenaltyWaiverException("Refund failed: " + refundResult.message());
            }
        } catch (Exception e) {
            log.error("Error processing refund for waiver: {}", waiver.getId(), e);
            throw new PenaltyWaiverException("Failed to process refund", e);
        }
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
