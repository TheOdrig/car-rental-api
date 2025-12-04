package com.akif.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum RentalStatus {
    REQUESTED("Requested", "Rental request created"),
    CONFIRMED("Confirmed", "Rental confirmed, payment authorized"),
    IN_USE("In Use", "Car picked up by customer"),
    RETURNED("Returned", "Car returned"),
    CANCELLED("Cancelled", "Rental cancelled");

    private final String displayName;
    private final String description;

    RentalStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public boolean canConfirm() {
        return this == REQUESTED;
    }


    public boolean canPickup() {
        return this == CONFIRMED;
    }

    public boolean canReturn() {
        return this == IN_USE;
    }


    public boolean canCancel() {
        return this == REQUESTED || this == CONFIRMED || this == IN_USE;
    }

    public static RentalStatus fromString(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }

        String trimmed = status.trim().toUpperCase();
        for (RentalStatus rentalStatus : values()) {
            if (rentalStatus.name().equals(trimmed) ||
                    rentalStatus.displayName.equalsIgnoreCase(status)) {
                return rentalStatus;
            }
        }
        throw new IllegalArgumentException("Invalid rental status: " + status);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
