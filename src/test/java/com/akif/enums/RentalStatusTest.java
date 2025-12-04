package com.akif.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RentalStatus Enum Tests")
class RentalStatusTest {

    @Nested
    @DisplayName("canConfirm Tests")
    class CanConfirmTests {

        @Test
        @DisplayName("Should return true when status is REQUESTED")
        void shouldReturnTrueWhenStatusIsRequested() {
            assertThat(RentalStatus.REQUESTED.canConfirm()).isTrue();
        }

        @Test
        @DisplayName("Should return false when status is CONFIRMED")
        void shouldReturnFalseWhenStatusIsConfirmed() {
            assertThat(RentalStatus.CONFIRMED.canConfirm()).isFalse();
        }

        @Test
        @DisplayName("Should return false when status is IN_USE")
        void shouldReturnFalseWhenStatusIsInUse() {
            assertThat(RentalStatus.IN_USE.canConfirm()).isFalse();
        }

        @Test
        @DisplayName("Should return false when status is RETURNED")
        void shouldReturnFalseWhenStatusIsReturned() {
            assertThat(RentalStatus.RETURNED.canConfirm()).isFalse();
        }

        @Test
        @DisplayName("Should return false when status is CANCELLED")
        void shouldReturnFalseWhenStatusIsCancelled() {
            assertThat(RentalStatus.CANCELLED.canConfirm()).isFalse();
        }
    }

    @Nested
    @DisplayName("canPickup Tests")
    class CanPickupTests {

        @Test
        @DisplayName("Should return true when status is CONFIRMED")
        void shouldReturnTrueWhenStatusIsConfirmed() {
            assertThat(RentalStatus.CONFIRMED.canPickup()).isTrue();
        }

        @Test
        @DisplayName("Should return false when status is REQUESTED")
        void shouldReturnFalseWhenStatusIsRequested() {
            assertThat(RentalStatus.REQUESTED.canPickup()).isFalse();
        }

        @Test
        @DisplayName("Should return false when status is IN_USE")
        void shouldReturnFalseWhenStatusIsInUse() {
            assertThat(RentalStatus.IN_USE.canPickup()).isFalse();
        }
    }

    @Nested
    @DisplayName("canReturn Tests")
    class CanReturnTests {

        @Test
        @DisplayName("Should return true when status is IN_USE")
        void shouldReturnTrueWhenStatusIsInUse() {
            assertThat(RentalStatus.IN_USE.canReturn()).isTrue();
        }

        @Test
        @DisplayName("Should return false when status is CONFIRMED")
        void shouldReturnFalseWhenStatusIsConfirmed() {
            assertThat(RentalStatus.CONFIRMED.canReturn()).isFalse();
        }

        @Test
        @DisplayName("Should return false when status is RETURNED")
        void shouldReturnFalseWhenStatusIsReturned() {
            assertThat(RentalStatus.RETURNED.canReturn()).isFalse();
        }
    }

    @Nested
    @DisplayName("canCancel Tests")
    class CanCancelTests {

        @Test
        @DisplayName("Should return true when status is REQUESTED")
        void shouldReturnTrueWhenStatusIsRequested() {
            assertThat(RentalStatus.REQUESTED.canCancel()).isTrue();
        }

        @Test
        @DisplayName("Should return true when status is CONFIRMED")
        void shouldReturnTrueWhenStatusIsConfirmed() {
            assertThat(RentalStatus.CONFIRMED.canCancel()).isTrue();
        }

        @Test
        @DisplayName("Should return true when status is IN_USE")
        void shouldReturnTrueWhenStatusIsInUse() {
            assertThat(RentalStatus.IN_USE.canCancel()).isTrue();
        }

        @Test
        @DisplayName("Should return false when status is RETURNED")
        void shouldReturnFalseWhenStatusIsReturned() {
            assertThat(RentalStatus.RETURNED.canCancel()).isFalse();
        }

        @Test
        @DisplayName("Should return false when status is CANCELLED")
        void shouldReturnFalseWhenStatusIsCancelled() {
            assertThat(RentalStatus.CANCELLED.canCancel()).isFalse();
        }
    }

    @Nested
    @DisplayName("fromString Tests")
    class FromStringTests {

        @Test
        @DisplayName("Should convert REQUESTED string to enum")
        void shouldConvertRequestedStringToEnum() {
            assertThat(RentalStatus.fromString("REQUESTED")).isEqualTo(RentalStatus.REQUESTED);
            assertThat(RentalStatus.fromString("requested")).isEqualTo(RentalStatus.REQUESTED);
            assertThat(RentalStatus.fromString("Requested")).isEqualTo(RentalStatus.REQUESTED);
        }

        @Test
        @DisplayName("Should convert display name to enum")
        void shouldConvertDisplayNameToEnum() {
            assertThat(RentalStatus.fromString("Requested")).isEqualTo(RentalStatus.REQUESTED);
        }

        @Test
        @DisplayName("Should throw exception when status is null")
        void shouldThrowExceptionWhenStatusIsNull() {
            assertThatThrownBy(() -> RentalStatus.fromString(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when status is empty")
        void shouldThrowExceptionWhenStatusIsEmpty() {
            assertThatThrownBy(() -> RentalStatus.fromString(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when status is invalid")
        void shouldThrowExceptionWhenStatusIsInvalid() {
            assertThatThrownBy(() -> RentalStatus.fromString("INVALID_STATUS"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid rental status");
        }
    }
}
