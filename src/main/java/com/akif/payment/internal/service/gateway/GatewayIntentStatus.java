package com.akif.payment.internal.service.gateway;

public enum GatewayIntentStatus {
    REQUIRES_PAYMENT_METHOD,
    REQUIRES_ACTION,
    PROCESSING,
    REQUIRES_CAPTURE,
    SUCCEEDED,
    CANCELED;

    public static GatewayIntentStatus fromStripe(String status) {
        if (status == null) {
            return REQUIRES_PAYMENT_METHOD;
        }
        return switch (status) {
            case "requires_action", "requires_confirmation" -> REQUIRES_ACTION;
            case "processing" -> PROCESSING;
            case "requires_capture" -> REQUIRES_CAPTURE;
            case "succeeded" -> SUCCEEDED;
            case "canceled" -> CANCELED;
            default -> REQUIRES_PAYMENT_METHOD;
        };
    }
}
