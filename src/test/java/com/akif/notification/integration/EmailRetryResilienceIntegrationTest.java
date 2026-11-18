package com.akif.notification.integration;

import com.akif.CarRentalApplication;
import com.akif.notification.EmailNotificationService;
import com.akif.notification.domain.EmailType;
import com.akif.notification.internal.dto.EmailMessage;
import com.akif.notification.internal.exception.EmailSendException;
import com.akif.notification.internal.service.email.IEmailSender;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Email retries with Spring Framework resilience")
class EmailRetryResilienceIntegrationTest {

    @Autowired
    private EmailNotificationService emailNotificationService;

    @MockitoBean
    private IEmailSender emailSender;

    private EmailSendException smtpFailure() {
        return new EmailSendException("user@example.com", EmailType.PASSWORD_RESET, "451", "temporary failure");
    }

    @Test
    @DisplayName("wraps the notification service in a resilience proxy")
    void serviceIsProxied() {
        assertThat(AopUtils.isAopProxy(emailNotificationService)).isTrue();
    }

    @Test
    @DisplayName("retries a transient send failure and succeeds on a later attempt")
    void retriesTransientFailure() {
        doThrow(smtpFailure()).doThrow(smtpFailure()).doNothing().when(emailSender).send(any(EmailMessage.class));

        emailNotificationService.sendPasswordResetEmail("user@example.com", "https://rent.example.com/reset?token=t");

        verify(emailSender, times(3)).send(any(EmailMessage.class));
    }

    @Test
    @DisplayName("gives up after the initial attempt plus three retries and rethrows so the publication stays incomplete")
    void givesUpAfterFourAttempts() {
        doThrow(smtpFailure()).when(emailSender).send(any(EmailMessage.class));

        assertThatThrownBy(() -> emailNotificationService.sendPasswordResetEmail(
                "user@example.com", "https://rent.example.com/reset?token=t"))
                .isInstanceOf(EmailSendException.class);

        verify(emailSender, times(4)).send(any(EmailMessage.class));
    }

    @Test
    @DisplayName("does not retry failures that are not send errors")
    void doesNotRetryOtherFailures() {
        doThrow(new IllegalStateException("template broken")).when(emailSender).send(any(EmailMessage.class));

        assertThatThrownBy(() -> emailNotificationService.sendPasswordResetEmail(
                "user@example.com", "https://rent.example.com/reset?token=t"))
                .isInstanceOf(IllegalStateException.class);

        verify(emailSender, times(1)).send(any(EmailMessage.class));
    }

    @Test
    @DisplayName("sends once when the first attempt succeeds")
    void sendsOnceOnSuccess() {
        doNothing().when(emailSender).send(any(EmailMessage.class));

        emailNotificationService.sendPasswordResetEmail("user@example.com", "https://rent.example.com/reset?token=t");

        verify(emailSender, times(1)).send(any(EmailMessage.class));
    }
}
