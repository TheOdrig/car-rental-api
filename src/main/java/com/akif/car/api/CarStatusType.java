package com.akif.car.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
@JsonFormat(shape=JsonFormat.Shape.STRING)
public enum CarStatusType {

    AVAILABLE("Available", "Car is available for rental"),
    RENTED("Rented", "Car is currently rented out"),
    MAINTENANCE("Maintenance", "Car is under maintenance"),
    RESERVED("Reserved", "Car is reserved for a customer"),
    DAMAGED("Damaged", "Car is damaged and needs repair"),
    INSPECTION("Inspection", "Car is being inspected");

    private final String displayName;
    private final String description;
    CarStatusType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public boolean isAvailable() {
        return this == AVAILABLE;
    }

    public boolean canBeReserved() {
        return this == AVAILABLE;
    }

    public boolean canBeMaintained() {
        return this == AVAILABLE || this == DAMAGED;
    }

    public boolean isInactive() {
        return this == MAINTENANCE || this == RENTED;
    }

    public boolean requiresAttention() {
        return this == DAMAGED || this == INSPECTION;
    }

    public static CarStatusType[] getUnavailableStatuses() {
        return new CarStatusType[]{RENTED, MAINTENANCE, DAMAGED, INSPECTION};
    }

    public static CarStatusType fromString(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }

        for (CarStatusType type : values()) {
            if (type.name().equalsIgnoreCase(status.trim()) ||
                    type.displayName.equalsIgnoreCase(status.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid car status: " + status);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
