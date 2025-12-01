package com.akif.service.gateway;

public record CheckoutSessionResult(
        String sessionId,
        String sessionUrl,
        String idempotencyKey
) {
}
