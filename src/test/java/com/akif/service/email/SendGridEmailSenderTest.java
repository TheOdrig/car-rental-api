package com.akif.service.email;

import com.akif.config.EmailProperties;
import com.akif.dto.email.EmailMessage;
import com.akif.shared.enums.EmailType;
import com.akif.exception.EmailSendException;
import com.akif.service.email.impl.SendGridEmailSender;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SendGridEmailSender Tests")
class SendGridEmailSenderTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailProperties emailProperties;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private SendGridEmailSender sendGridEmailSender;

    private EmailMessage testMessage;

    @BeforeEach
    void setUp() {
        testMessage = new EmailMessage(
            "customer@example.com",
            "Test Subject",
            "<html><body>Test Body</body></html>",
            EmailType.RENTAL_CONFIRMATION,
            1L
        );

        when(emailProperties.getFrom()).thenReturn("noreply@rentacar.com");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Nested
    @DisplayName("Successful Email Sending")
    class SuccessfulEmailSending {

        @Test
        @DisplayName("Should successfully send email via JavaMailSender")
        void shouldSuccessfullySendEmail() {
            sendGridEmailSender.send(testMessage);

            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should send email with correct from address")
        void shouldSendEmailWithCorrectFromAddress() {
            sendGridEmailSender.send(testMessage);

            verify(emailProperties).getFrom();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should send email for all email types")
        void shouldSendEmailForAllEmailTypes() {
            for (EmailType type : EmailType.values()) {
                EmailMessage message = new EmailMessage(
                    "test@example.com",
                    "Test Subject",
                    "<html><body>Test Body</body></html>",
                    type,
                    1L
                );

                sendGridEmailSender.send(message);
            }

            verify(mailSender, times(EmailType.values().length)).send(mimeMessage);
        }
    }

    @Nested
    @DisplayName("Email Sending Failures")
    class EmailSendingFailures {

        @Test
        @DisplayName("Should throw EmailSendException when MailException occurs")
        void shouldThrowEmailSendExceptionWhenMailExceptionOccurs() {
            doThrow(new MailException("SMTP error") {})
                .when(mailSender).send(any(MimeMessage.class));

            assertThatThrownBy(() -> sendGridEmailSender.send(testMessage))
                .isInstanceOf(EmailSendException.class)
                .hasMessageContaining("Failed to send email");

            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should extract SMTP error code 550")
        void shouldExtractSmtpErrorCode550() {
            doThrow(new MailException("550 User not found") {})
                .when(mailSender).send(any(MimeMessage.class));

            assertThatThrownBy(() -> sendGridEmailSender.send(testMessage))
                .isInstanceOf(EmailSendException.class)
                .satisfies(ex -> {
                    EmailSendException emailEx = (EmailSendException) ex;
                    assertThat(emailEx.getSmtpErrorCode()).isEqualTo("550");
                });
        }

        @Test
        @DisplayName("Should extract SMTP error code 421")
        void shouldExtractSmtpErrorCode421() {
            doThrow(new MailException("421 Service not available") {})
                .when(mailSender).send(any(MimeMessage.class));

            assertThatThrownBy(() -> sendGridEmailSender.send(testMessage))
                .isInstanceOf(EmailSendException.class)
                .satisfies(ex -> {
                    EmailSendException emailEx = (EmailSendException) ex;
                    assertThat(emailEx.getSmtpErrorCode()).isEqualTo("421");
                });
        }

        @Test
        @DisplayName("Should extract SMTP error code 451")
        void shouldExtractSmtpErrorCode451() {
            doThrow(new MailException("451 Temporary failure") {})
                .when(mailSender).send(any(MimeMessage.class));

            assertThatThrownBy(() -> sendGridEmailSender.send(testMessage))
                .isInstanceOf(EmailSendException.class)
                .satisfies(ex -> {
                    EmailSendException emailEx = (EmailSendException) ex;
                    assertThat(emailEx.getSmtpErrorCode()).isEqualTo("451");
                });
        }

        @Test
        @DisplayName("Should return UNKNOWN for unrecognized SMTP error")
        void shouldReturnUnknownForUnrecognizedSmtpError() {
            doThrow(new MailException("999 Unknown error") {})
                .when(mailSender).send(any(MimeMessage.class));

            assertThatThrownBy(() -> sendGridEmailSender.send(testMessage))
                .isInstanceOf(EmailSendException.class)
                .satisfies(ex -> {
                    EmailSendException emailEx = (EmailSendException) ex;
                    assertThat(emailEx.getSmtpErrorCode()).isEqualTo("UNKNOWN");
                });
        }

        @Test
        @DisplayName("Should include recipient and email type in exception")
        void shouldIncludeRecipientAndEmailTypeInException() {
            doThrow(new MailException("SMTP error") {})
                .when(mailSender).send(any(MimeMessage.class));

            assertThatThrownBy(() -> sendGridEmailSender.send(testMessage))
                .isInstanceOf(EmailSendException.class)
                .satisfies(ex -> {
                    EmailSendException emailEx = (EmailSendException) ex;
                    assertThat(emailEx.getRecipient()).isEqualTo("customer@example.com");
                    assertThat(emailEx.getEmailType()).isEqualTo(EmailType.RENTAL_CONFIRMATION);
                });
        }
    }
}
