package com.akif.rental.internal.service.penalty;

import com.akif.payment.api.PaymentDto;
import com.akif.rental.domain.model.Rental;
import com.akif.payment.api.PaymentResult;

import java.math.BigDecimal;

public interface PenaltyPaymentService {

    PaymentDto createPenaltyPayment(Rental rental, BigDecimal penaltyAmount);

    PaymentResult chargePenalty(Long paymentId, Long userId);

    void handleFailedPenaltyPayment(PaymentDto payment, String failureReason);
}