package com.akif.payment.internal.service.gateway;

public record GatewayIntent(
        String id,
        String clientSecret,
        GatewayIntentStatus status,
        String paymentMethodId,
        String customerId,
        long amountMinor,
        String currency
) {
}
