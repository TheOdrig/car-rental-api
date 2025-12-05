package com.akif.service.penalty;

import com.akif.model.Payment;
import com.akif.model.Rental;
import com.akif.service.gateway.PaymentResult;

import java.math.BigDecimal;

public interface IPenaltyPaymentService {

    Payment createPenaltyPayment(Rental rental, BigDecimal penaltyAmount);

    PaymentResult chargePenalty(Payment penaltyPayment);

    void handleFailedPenaltyPayment(Payment penaltyPayment);
}
