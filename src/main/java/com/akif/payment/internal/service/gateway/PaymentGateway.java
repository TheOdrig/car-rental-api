package com.akif.payment.internal.service.gateway;

import com.akif.payment.api.PaymentResult;
import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;

public interface PaymentGateway {

    GatewayIntent createIntent(IntentCommand command, String idempotencyKey);

    GatewayIntent retrieveIntent(String intentId);

    PaymentResult capture(String intentId, String idempotencyKey);

    PaymentResult cancel(String intentId, String idempotencyKey);

    PaymentResult refund(String intentId, BigDecimal amount, CurrencyType currency, String idempotencyKey);

    PaymentResult chargeSavedMethod(ChargeCommand command, String idempotencyKey);
}
