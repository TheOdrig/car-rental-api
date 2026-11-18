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
    @Override
    public String toString() {
        return "GatewayIntent[id=" + id + ", clientSecret=[REDACTED], status=" + status + ", paymentMethodId=" + paymentMethodId + ", customerId=" + customerId + ", amountMinor=" + amountMinor + ", currency=" + currency + "]";
    }
}
