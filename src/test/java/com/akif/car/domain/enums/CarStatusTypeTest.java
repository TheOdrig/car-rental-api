package com.akif.car.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CarStatusType Enum Tests")
public class CarStatusTypeTest {

    @Test
    @DisplayName("isAvailable should return true only for AVAILABLE status")
    void isAvailable_ShouldReturnTrueOnlyForAvailable() {

        assertThat(CarStatusType.AVAILABLE.isAvailable()).isTrue();
        assertThat(CarStatusType.MAINTENANCE.isAvailable()).isFalse();
        assertThat(CarStatusType.RESERVED.isAvailable()).isFalse();
        assertThat(CarStatusType.DAMAGED.isAvailable()).isFalse();
        assertThat(CarStatusType.INSPECTION.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("canBeReserved should return true only for AVAILABLE status")
    void canBeReserved_ShouldReturnTrueOnlyForAvailable() {

        assertThat(CarStatusType.AVAILABLE.canBeReserved()).isTrue();
        assertThat(CarStatusType.MAINTENANCE.canBeReserved()).isFalse();
        assertThat(CarStatusType.RESERVED.canBeReserved()).isFalse();
        assertThat(CarStatusType.DAMAGED.canBeReserved()).isFalse();
        assertThat(CarStatusType.INSPECTION.canBeReserved()).isFalse();
    }

    @Test
    @DisplayName("isInactive should return true for MAINTENANCE and RENTED statuses")
    void isInactive_ShouldReturnTrueForMaintenanceAndRented() {

        assertThat(CarStatusType.MAINTENANCE.isInactive()).isTrue();
        assertThat(CarStatusType.RENTED.isInactive()).isTrue();
        assertThat(CarStatusType.AVAILABLE.isInactive()).isFalse();
        assertThat(CarStatusType.RESERVED.isInactive()).isFalse();
        assertThat(CarStatusType.DAMAGED.isInactive()).isFalse();
        assertThat(CarStatusType.INSPECTION.isInactive()).isFalse();
    }

    @Test
    @DisplayName("requiresAttention should return true for DAMAGED and INSPECTION statuses")
    void requiresAttention_ShouldReturnTrueForDamagedAndInspection() {

        assertThat(CarStatusType.DAMAGED.requiresAttention()).isTrue();
        assertThat(CarStatusType.INSPECTION.requiresAttention()).isTrue();
        assertThat(CarStatusType.AVAILABLE.requiresAttention()).isFalse();
        assertThat(CarStatusType.MAINTENANCE.requiresAttention()).isFalse();
        assertThat(CarStatusType.RESERVED.requiresAttention()).isFalse();
    }

    @Test
    @DisplayName("getUnavailableStatuses should return all non-available statuses")
    void getUnavailableStatuses_ShouldReturnAllNonAvailableStatuses() {

        CarStatusType[] unavailableStatuses = CarStatusType.getUnavailableStatuses();

        assertThat(unavailableStatuses).hasSize(4);
        assertThat(unavailableStatuses).containsExactlyInAnyOrder(
                CarStatusType.RENTED,
                CarStatusType.MAINTENANCE,
                CarStatusType.DAMAGED,
                CarStatusType.INSPECTION);
    }

    @ParameterizedTest
    @ValueSource(strings = { "AVAILABLE", " available ", " Available", "RENTED", " rented ", " Rented" })
    @DisplayName("fromString should parse valid status string")
    void fromString_WithValidString_ShouldReturnCorrectEnum(String statusString) {

        CarStatusType result = CarStatusType.fromString(statusString);

        assertThat(result).isNotNull();
        assertThat(result.name().equalsIgnoreCase(statusString.trim()) ||
                result.getDisplayName().equalsIgnoreCase(statusString.trim()))
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " " })
    @DisplayName("fromString should throw exception for invalid strings")
    void fromString_WithInvalidStrings_ShouldThrowException(String invalidString) {

        assertThatThrownBy(() -> CarStatusType.fromString(invalidString))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status cannot be null or empty");
    }

    @Test
    @DisplayName("fromString with null should throw exception")
    void fromString_WithNull_ShouldThrowException() {

        assertThatThrownBy(() -> CarStatusType.fromString(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Status cannot be null or empty");
    }

    @Test
    @DisplayName("toString should return display name")
    void toString_ShouldReturnDisplayName() {

        assertThat(CarStatusType.AVAILABLE.toString()).isEqualTo("Available");
        assertThat(CarStatusType.MAINTENANCE.toString()).isEqualTo("Maintenance");
    }

    @Test
    @DisplayName("getDisplayName should return correct display name")
    void getDisplayName_ShouldReturnCorrectDisplayName() {

        assertThat(CarStatusType.AVAILABLE.getDisplayName()).isEqualTo("Available");
        assertThat(CarStatusType.MAINTENANCE.getDisplayName()).isEqualTo("Maintenance");
    }
}
