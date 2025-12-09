package com.akif.shared.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum PaymentStatus {
    PENDING("Pending", "Payment pending"),
    AUTHORIZED("Authorized", "Payment authorized, not yet captured"),
    CAPTURED("Captured", "Payment captured"),
    FAILED("Failed", "Payment failed"),
    REFUNDED("Refunded", "Payment refunded");

    private final String displayName;
    private final String description;

    PaymentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public boolean isSuccessful() {
        return this == AUTHORIZED || this == CAPTURED;
    }

    public boolean canRefund() {
        return this == CAPTURED;
    }

    public static PaymentStatus fromString(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment status cannot be null or empty");
        }

        String trimmed = status.trim().toUpperCase();
        for (PaymentStatus paymentStatus : values()) {
            if (paymentStatus.name().equals(trimmed) ||
                    paymentStatus.displayName.equalsIgnoreCase(status)) {
                return paymentStatus;
            }
        }
        throw new IllegalArgumentException("Invalid payment status: " + status);
    }

    @Override
    public String toString() {
        return displayName;
    }
}