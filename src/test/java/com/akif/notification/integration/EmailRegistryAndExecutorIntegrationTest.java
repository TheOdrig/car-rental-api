package com.akif.notification.integration;

import com.akif.CarRentalApplication;
import com.akif.auth.api.PasswordResetService;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.notification.EmailNotificationService;
import com.akif.rental.api.RentalConfirmedEvent;
import com.akif.shared.enums.CurrencyType;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Email event registry and executor")
class EmailRegistryAndExecutorIntegrationTest {

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private EmailNotificationService emailNotificationService;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM car_rental.event_publication");
    }

    @Test
    @DisplayName("a password reset request leaves no reset link or token in event_publication")
    void resetLinkIsNotPersistedInTheRegistry() {
        User user = userRepository.save(TestDataBuilder.createTestUser("resetmailuser", com.akif.shared.enums.Role.USER));
        try {
            passwordResetService.requestPasswordReset(user.getEmail());

            ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
            await().atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> verify(emailNotificationService).sendPasswordResetEmail(anyString(), link.capture()));
            String token = link.getValue().substring(link.getValue().indexOf("token=") + 6);

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM car_rental.event_publication WHERE serialized_event LIKE ? "
                            + "OR serialized_event LIKE '%reset-password%' OR event_type LIKE '%PasswordResetRequested%'",
                    Integer.class, "%" + token + "%")).isZero();
        } finally {
            jdbcTemplate.update("DELETE FROM car_rental.password_reset_tokens");
            userRepository.delete(user);
        }
    }

    @Test
    @DisplayName("completed publications are deleted instead of kept with their payload")
    void completedPublicationsAreDeleted() {
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event()));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> verify(emailNotificationService).sendRentalConfirmation(any()));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM car_rental.event_publication", Integer.class)).isZero());
    }

    @Test
    @DisplayName("email listeners run without a database transaction")
    void listenersAreNotTransactional() {
        AtomicReference<Boolean> transactional = new AtomicReference<>();
        doAnswer(invocation -> {
            transactional.set(TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).when(emailNotificationService).sendRentalConfirmation(any());

        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event()));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(transactional.get()).isNotNull());
        assertThat(transactional.get()).isFalse();
    }

    @Test
    @DisplayName("the email executor is capped below the database pool and SMTP calls have timeouts")
    void executorCapAndTimeouts() throws Exception {
        Properties properties = new Properties();
        try (InputStream in = new ClassPathResource("application.properties").getInputStream()) {
            properties.load(in);
        }

        assertThat(Integer.parseInt(properties.getProperty("email.async.max-pool-size")))
                .isLessThan(Integer.parseInt(properties.getProperty("spring.datasource.hikari.maximum-pool-size")));
        assertThat(properties.getProperty("spring.mail.properties.mail.smtp.connectiontimeout")).isNotBlank();
        assertThat(properties.getProperty("spring.mail.properties.mail.smtp.timeout")).isNotBlank();
    }

    private RentalConfirmedEvent event() {
        return new RentalConfirmedEvent(
 77L,
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
}
