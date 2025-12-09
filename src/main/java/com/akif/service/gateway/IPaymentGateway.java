package com.akif.service.gateway;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;

public interface IPaymentGateway {

    PaymentResult authorize(BigDecimal amount, CurrencyType currency, String customerId);

    PaymentResult capture(String transactionId, BigDecimal amount);

    PaymentResult refund(String transactionId, BigDecimal amount);
}