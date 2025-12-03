package com.akif.service.email;

import com.akif.dto.email.EmailMessage;
import com.akif.enums.EmailType;
import com.akif.service.email.impl.MockEmailSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("MockEmailSender Tests")
class MockEmailSenderTest {

    @InjectMocks
    private MockEmailSender mockEmailSender;

    @Test
    @DisplayName("Should log email without throwing exception")
    void shouldLogEmailWithoutThrowingException() {
        EmailMessage message = new EmailMessage(
            "test@example.com",
            "Test Subject",
            "<html><body>Test Body</body></html>",
            EmailType.RENTAL_CONFIRMATION,
            1L
        );

        assertThatCode(() -> mockEmailSender.send(message))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle null body gracefully")
    void shouldHandleNullBodyGracefully() {
        EmailMessage message = new EmailMessage(
            "test@example.com",
            "Test Subject",
            null,
            EmailType.RENTAL_CONFIRMATION,
            1L
        );

        assertThatCode(() -> mockEmailSender.send(message))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle all email types")
    void shouldHandleAllEmailTypes() {
        for (EmailType type : EmailType.values()) {
            EmailMessage message = new EmailMessage(
                "test@example.com",
                "Test Subject",
                "<html><body>Test Body</body></html>",
                type,
                1L
            );

            assertThatCode(() -> mockEmailSender.send(message))
                .doesNotThrowAnyException();
        }
    }
}
