package com.akif.notification.integration;

import com.akif.CarRentalApplication;
import com.akif.notification.EmailNotificationService;
import com.akif.rental.api.RentalConfirmedEvent;
import com.akif.shared.enums.CurrencyType;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Event publication registry")
class EventPublicationRegistryIntegrationTest {

    private static final String EMAIL_LISTENER = "%EmailEventListener%";

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IncompleteEventPublications incompleteEventPublications;

    @MockitoBean
    private EmailNotificationService emailNotificationService;

    @BeforeEach
    @AfterEach
    void cleanRegistry() {
        jdbcTemplate.update("DELETE FROM car_rental.event_publication");
    }

    private RentalConfirmedEvent event() {
        return new RentalConfirmedEvent(
                42L,
                "customer@example.com",
                LocalDateTime.now(),
                "Toyota",
                "Corolla",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(5),
                new BigDecimal("2000.00"),
                CurrencyType.TRY,
                null);
    }

    private int incompleteEmailPublications() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM car_rental.event_publication WHERE completion_date IS NULL AND listener_id LIKE ?",
                Integer.class, EMAIL_LISTENER);
    }

    private int completedEmailPublications() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM car_rental.event_publication WHERE completion_date IS NOT NULL AND listener_id LIKE ?",
                Integer.class, EMAIL_LISTENER);
    }

    @Test
    @DisplayName("runs the listener only after the publishing transaction commits")
    void listenerRunsAfterCommit() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event());
            verifyNoInteractions(emailNotificationService);
        });

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                verify(emailNotificationService, times(1)).sendRentalConfirmation(any()));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(incompleteEmailPublications()).isZero());
        assertThat(incompleteEmailPublications()).isZero();
    }

    @Test
    @DisplayName("never runs the listener and stores no publication when the publishing transaction rolls back")
    void rollbackSuppressesListener() throws InterruptedException {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event());
            status.setRollbackOnly();
        });

        Thread.sleep(1000);
        verify(emailNotificationService, never()).sendRentalConfirmation(any());
        assertThat(incompleteEmailPublications()).isZero();
        assertThat(completedEmailPublications()).isZero();
    }

    @Test
    @DisplayName("a failing listener leaves an incomplete publication that is republished and then completes")
    void failedListenerIsRepublished() {
        doThrow(new IllegalStateException("smtp unavailable"))
                .doNothing()
                .when(emailNotificationService).sendRentalConfirmation(any());

        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event()));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            verify(emailNotificationService, times(1)).sendRentalConfirmation(any());
            assertThat(incompleteEmailPublications()).isEqualTo(1);
        });
        assertThat(completedEmailPublications()).isZero();

        incompleteEventPublications.resubmitIncompletePublications(publication -> true);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            verify(emailNotificationService, times(2)).sendRentalConfirmation(any());
            assertThat(incompleteEmailPublications()).isZero();
        });
        assertThat(incompleteEmailPublications()).isZero();
    }

    @Test
    @DisplayName("an incomplete publication survives and a second resubmission does not redeliver a completed one")
    void completedPublicationIsNotRedelivered() {
        doNothing().when(emailNotificationService).sendRentalConfirmation(any());

        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event()));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(incompleteEmailPublications()).isZero());

        incompleteEventPublications.resubmitIncompletePublications(publication -> true);

        verify(emailNotificationService, times(1)).sendRentalConfirmation(any());
    }
}
