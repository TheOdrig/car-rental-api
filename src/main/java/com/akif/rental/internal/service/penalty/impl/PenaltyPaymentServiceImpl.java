package com.akif.rental.internal.service.penalty.impl;

import com.akif.payment.api.CreatePaymentRequest;
import com.akif.payment.api.PaymentDto;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.api.PaymentService;
import com.akif.rental.api.PenaltyChargeFailedEvent;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Clock;
import java.time.LocalDateTime;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.service.penalty.PenaltyPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PenaltyPaymentServiceImpl implements PenaltyPaymentService {

    private final Clock clock;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher eventPublisher;

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
    @Transactional(propagation = Propagation.SUPPORTS)
    public PaymentResult chargePenalty(Long paymentId, Long userId) {
        log.debug("Attempting to charge penalty payment: paymentId={}, userId={}", paymentId, userId);

        String customerId = userId.toString();
        PaymentResult result = paymentService.chargePayment(paymentId);

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
    @Transactional
    public void handleFailedPenaltyPayment(PaymentDto payment, String failureReason) {
        log.warn("Penalty payment could not be collected and needs manual follow-up: paymentId={}, rentalId={}, reason={}",
                payment.id(), payment.rentalId(), failureReason);

        eventPublisher.publishEvent(new PenaltyChargeFailedEvent(
                payment.rentalId(),
                payment.id(),
                payment.userEmail(),
                LocalDateTime.now(clock),
                payment.amount(),
                payment.currency(),
                failureReason));
    }
}
