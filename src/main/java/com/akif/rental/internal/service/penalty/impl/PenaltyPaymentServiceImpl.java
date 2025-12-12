package com.akif.rental.internal.service.penalty.impl;

import com.akif.payment.api.CreatePaymentRequest;
import com.akif.payment.api.PaymentDto;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.api.PaymentService;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.service.penalty.PenaltyPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PenaltyPaymentServiceImpl implements PenaltyPaymentService {

    private final PaymentService paymentService;

    @Override
    @Transactional
    public PaymentDto createPenaltyPayment(Rental rental, BigDecimal penaltyAmount) {
        log.debug("Creating penalty payment for rental: {} with amount: {} {}", 
                rental.getId(), penaltyAmount, rental.getCurrency());

        if (penaltyAmount == null || penaltyAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Penalty amount must be positive");
        }

        CreatePaymentRequest request = new CreatePaymentRequest(
                rental.getId(),
                rental.getUserEmail(),
                rental.getCarLicensePlate(),
                penaltyAmount,
                rental.getCurrency(),
                "PENALTY"
        );

        PaymentDto paymentDto = paymentService.createPayment(request);
        
        log.info("Created penalty payment: ID={}, Rental ID={}, Amount={} {}", 
                paymentDto.id(), rental.getId(), penaltyAmount, rental.getCurrency());

        return paymentDto;
    }

    @Override
    @Transactional
    public PaymentResult chargePenalty(Long paymentId, Long userId) {
        log.debug("Attempting to charge penalty payment: paymentId={}, userId={}", paymentId, userId);

        String customerId = userId.toString();
        PaymentResult result = paymentService.chargePayment(paymentId, customerId);

        if (result.success()) {
            log.info("Successfully charged penalty payment: ID={}, Transaction ID={}", 
                    paymentId, result.transactionId());
        } else {
            log.warn("Failed to charge penalty payment: ID={}, Reason: {}", 
                    paymentId, result.message());
        }

        return result;
    }

    @Override
    public void handleFailedPenaltyPayment(Long paymentId, Long rentalId, String userEmail, String failureReason) {
        log.debug("Handling failed penalty payment: paymentId={}", paymentId);

        log.warn("[ADMIN NOTIFICATION] Failed penalty payment requires manual processing: " +
                "Payment ID={}, Rental ID={}, Customer Email={}, Failure Reason: {}", 
                paymentId,
                rentalId,
                userEmail,
                failureReason);

        log.info("Marked penalty payment as PENDING for manual processing: ID={}", paymentId);
    }
}

