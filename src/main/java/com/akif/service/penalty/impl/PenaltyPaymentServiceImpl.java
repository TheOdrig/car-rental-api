package com.akif.service.penalty.impl;

import com.akif.shared.enums.PaymentStatus;
import com.akif.model.Payment;
import com.akif.model.Rental;
import com.akif.repository.PaymentRepository;
import com.akif.service.gateway.IPaymentGateway;
import com.akif.service.gateway.PaymentResult;
import com.akif.service.penalty.IPenaltyPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PenaltyPaymentServiceImpl implements IPenaltyPaymentService {

    private final PaymentRepository paymentRepository;
    private final IPaymentGateway paymentGateway;

    @Override
    @Transactional
    public Payment createPenaltyPayment(Rental rental, BigDecimal penaltyAmount) {
        log.debug("Creating penalty payment for rental: {} with amount: {} {}", 
                 rental.getId(), penaltyAmount, rental.getCurrency());

        if (penaltyAmount == null || penaltyAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Penalty amount must be positive");
        }

        Payment penaltyPayment = Payment.builder()
                .rental(rental)
                .amount(penaltyAmount)
                .currency(rental.getCurrency())
                .status(PaymentStatus.PENDING)
                .paymentMethod("PENALTY")
                .isDeleted(false)
                .build();

        Payment savedPayment = paymentRepository.save(penaltyPayment);
        
        log.info("Created penalty payment: ID={}, Rental ID={}, Amount={} {}", 
                savedPayment.getId(), rental.getId(), penaltyAmount, rental.getCurrency());

        return savedPayment;
    }

    @Override
    @Transactional
    public PaymentResult chargePenalty(Payment penaltyPayment) {
        log.debug("Attempting to charge penalty payment: {}", penaltyPayment.getId());

        if (penaltyPayment.getRental() == null || penaltyPayment.getRental().getUser() == null) {
            throw new IllegalArgumentException("Penalty payment must have associated rental and user");
        }

        try {
            String customerId = penaltyPayment.getRental().getUser().getId().toString();

            PaymentResult authorizeResult = paymentGateway.authorize(
                    penaltyPayment.getAmount(),
                    penaltyPayment.getCurrency(),
                    customerId
            );

            if (!authorizeResult.success()) {
                log.warn("Failed to authorize penalty payment: ID={}, Reason: {}", 
                        penaltyPayment.getId(), authorizeResult.message());
                
                penaltyPayment.updateStatus(PaymentStatus.FAILED);
                penaltyPayment.setFailureReason(authorizeResult.message());
                penaltyPayment.setGatewayResponse(authorizeResult.message());
                paymentRepository.save(penaltyPayment);
                
                return authorizeResult;
            }

            PaymentResult captureResult = paymentGateway.capture(
                    authorizeResult.transactionId(),
                    penaltyPayment.getAmount()
            );

            if (captureResult.success()) {
                penaltyPayment.updateStatus(PaymentStatus.CAPTURED);
                penaltyPayment.setTransactionId(captureResult.transactionId());
                penaltyPayment.setGatewayResponse(captureResult.message());
                paymentRepository.save(penaltyPayment);

                log.info("Successfully charged penalty payment: ID={}, Transaction ID={}", 
                        penaltyPayment.getId(), captureResult.transactionId());
                
                return captureResult;

            } else {
                log.warn("Failed to capture penalty payment: ID={}, Reason: {}", 
                        penaltyPayment.getId(), captureResult.message());
                
                penaltyPayment.updateStatus(PaymentStatus.FAILED);
                penaltyPayment.setFailureReason(captureResult.message());
                penaltyPayment.setGatewayResponse(captureResult.message());
                paymentRepository.save(penaltyPayment);

            }
            return captureResult;
        } catch (Exception e) {
            log.error("Exception while charging penalty payment: ID={}, Error: {}", 
                     penaltyPayment.getId(), e.getMessage(), e);
            
            penaltyPayment.updateStatus(PaymentStatus.FAILED);
            penaltyPayment.setFailureReason(e.getMessage());
            paymentRepository.save(penaltyPayment);

            return PaymentResult.failure("Payment gateway error: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void handleFailedPenaltyPayment(Payment penaltyPayment) {
        log.debug("Handling failed penalty payment: {}", penaltyPayment.getId());

        penaltyPayment.updateStatus(PaymentStatus.PENDING);
        paymentRepository.save(penaltyPayment);

        log.warn("[ADMIN NOTIFICATION] Failed penalty payment requires manual processing: " +
                "Payment ID={}, Rental ID={}, Amount={} {}, Customer Email={}, Failure Reason: {}", 
                penaltyPayment.getId(),
                penaltyPayment.getRental().getId(),
                penaltyPayment.getAmount(),
                penaltyPayment.getCurrency(),
                penaltyPayment.getRental().getUser().getEmail(),
                penaltyPayment.getFailureReason());

        log.info("Marked penalty payment as PENDING for manual processing: ID={}", 
                penaltyPayment.getId());
    }
}
