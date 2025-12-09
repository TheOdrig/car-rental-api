package com.akif.shared.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
@JsonFormat(shape=JsonFormat.Shape.STRING)
public enum CarStatusType {

    AVAILABLE("Available", "Car is available for sale", true, false),
    SOLD("Sold", "Car has been sold to a customer", false, true),
    MAINTENANCE("Maintenance", "Car is under maintenance", false, false),
    RESERVED("Reserved", "Car is reserved for a customer", false, false),
    DAMAGED("Damaged", "Car is damaged and needs repair", false, false),
    INSPECTION("Inspection", "Car is being inspected", false, false);

    private final String displayName;
    private final String description;
    private final boolean availableForSale;
    private final boolean sold;

    CarStatusType(String displayName, String description, boolean availableForSale, boolean sold) {
        this.displayName = displayName;
        this.description = description;
        this.availableForSale = availableForSale;
        this.sold = sold;
    }


    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public boolean isAvailable() {
        return this == AVAILABLE;
    }

    public boolean canBeSold() {
        return this == AVAILABLE || this == RESERVED;
    }

    public boolean canBeReserved() {
        return this == AVAILABLE;
    }

    public boolean canBeMaintained() {
        return this == AVAILABLE || this == DAMAGED;
    }

    public boolean isInactive() {
        return this == SOLD || this == MAINTENANCE;
    }

    public boolean requiresAttention() {
        return this == DAMAGED || this == INSPECTION;
    }


    public static CarStatusType[] getUnavailableStatuses() {
        return new CarStatusType[]{SOLD, MAINTENANCE, DAMAGED, INSPECTION};
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
