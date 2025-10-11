package com.akif.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InvalidStatusTransitionException Unit Tests")
class InvalidStatusTransitionExceptionTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with message and BAD_REQUEST status")
        void shouldCreateExceptionWithMessageAndBadRequestStatus() {

            String message = "Invalid status transition from AVAILABLE to SOLD";

            InvalidStatusTransitionException exception = new InvalidStatusTransitionException(message);

            assertThat(exception.getErrorCode()).isEqualTo("INVALID_STATUS_TRANSITION");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should create exception with custom HTTP status")
        void shouldCreateExceptionWithCustomHttpStatus() {

            String message = "Status transition not allowed";
            HttpStatus customStatus = HttpStatus.FORBIDDEN;

            InvalidStatusTransitionException exception = new InvalidStatusTransitionException(message, customStatus);

            assertThat(exception.getErrorCode()).isEqualTo("INVALID_STATUS_TRANSITION");
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("Inheritance Tests")
    class InheritanceTests {

        @Test
        @DisplayName("Should extend BaseException")
        void shouldExtendBaseException() {

            InvalidStatusTransitionException exception = new InvalidStatusTransitionException("Test message");

            assertThat(exception).isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("Should be throwable")
        void shouldBeThrowable() {

            assertThatThrownBy(() -> {
                throw new InvalidStatusTransitionException("Test status transition error");
            }).isInstanceOf(InvalidStatusTransitionException.class)
              .hasMessage("Test status transition error");
        }
    }
}
