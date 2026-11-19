package com.akif.payment.integration;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.e2e.infrastructure.TestEventCaptor;
import com.akif.payment.api.CreatePaymentRequest;
import com.akif.payment.api.PaymentCancelledEvent;
import com.akif.payment.api.PaymentDto;
import com.akif.payment.api.PaymentIntentResult;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.api.PaymentService;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.Payment;
import com.akif.payment.domain.enums.PaymentKind;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.service.gateway.ChargeCommand;
import com.akif.payment.internal.service.gateway.ChargeResult;
import com.akif.payment.internal.service.gateway.GatewayIntent;
import com.akif.payment.internal.service.gateway.GatewayIntentStatus;
import com.akif.payment.internal.service.gateway.IntentCommand;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import com.akif.rental.api.PaymentRetryRequestedEvent;
import com.akif.rental.api.RentalCancelledEvent;
import com.akif.rental.api.RentalService;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.config.RentalProperties;
import com.akif.rental.internal.listener.PaymentCancelledListener;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.rental.internal.scheduler.ReminderScheduler;
import com.akif.rental.internal.scheduler.UnpaidRentalExpiryScheduler;
import com.akif.shared.enums.CurrencyType;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Payments with committed transactions")
class PaymentCommittedTransactionsIntegrationTest {

    @Autowired
    private com.akif.payment.internal.config.InstallationIdentity installation;

    private String keyPrefix() {
        return "car-rental:" + installation.id() + ":";
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RentalService rentalService;

    @Autowired
    private com.akif.car.api.DynamicPricingService dynamicPricingService;

    @Autowired
    private RentalProperties rentalProperties;

    @Autowired
    private UnpaidRentalExpiryScheduler expiryScheduler;

    @Autowired
    private PaymentCancelledListener paymentCancelledListener;

    @Autowired
    private TestEventCaptor eventCaptor;

    @MockitoSpyBean
    private PaymentGateway paymentGateway;

    @MockitoBean
    private ReminderScheduler reminderScheduler;

    private User customer;
    private Car car;
    private int originalWindow;

    @BeforeEach
    void setUp() {
        originalWindow = rentalProperties.getPaymentWindowDays();
        customer = userRepository.save(TestDataBuilder.createTestUser());
        car = carRepository.save(TestDataBuilder.createAvailableCar());
        eventCaptor.clear();
    }

    @AfterEach
    void cleanUp() {
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM car_rental.event_publication WHERE completion_date IS NULL", Long.class)).isZero());
        rentalProperties.setPaymentWindowDays(originalWindow);
        jdbcTemplate.update("DELETE FROM car_rental.payment_refunds");
        paymentRepository.deleteAll();
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Rental rental(LocalDate start, LocalDate end, RentalStatus status) {
        return rentalRepository.save(TestDataBuilder.createRental(car, customer, start, end, status));
    }

    private Payment rentalPayment(Rental rental, PaymentStatus status) {
        PaymentDto dto = paymentService.createPayment(new CreatePaymentRequest(
                rental.getId(), rental.getUserEmail(), rental.getCarLicensePlate(),
                rental.getTotalPrice(), rental.getCurrency(), "CARD"));
        Payment payment = paymentRepository.findById(dto.id()).orElseThrow();
        if (status == PaymentStatus.CAPTURED) {
            payment.attachIntent("pi_" + UUID.randomUUID().toString().substring(0, 8), "seed");
            payment.rememberCustomer("cus_saved");
            payment.rememberPaymentMethod("pm_saved");
            payment.capture();
            payment = paymentRepository.saveAndFlush(payment);
        }
        return payment;
    }

    private Payment penalty(Rental rental, String amount) {
        PaymentDto dto = paymentService.createPayment(new CreatePaymentRequest(
                rental.getId(), rental.getUserEmail(), rental.getCarLicensePlate(),
                new BigDecimal(amount), rental.getCurrency(), "PENALTY"));
        return paymentRepository.findById(dto.id()).orElseThrow();
    }

    private Rental capturedRental() {
        Rental rental = rental(LocalDate.now().plusDays(1), LocalDate.now().plusDays(4), RentalStatus.IN_USE);
        rentalPayment(rental, PaymentStatus.CAPTURED);
        return rental;
    }

    private TransactionTemplate outer() {
        return new TransactionTemplate(transactionManager);
    }

    private List<String> chargeKeys(int calls) {
        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(paymentGateway, times(calls)).chargeSavedMethod(any(ChargeCommand.class), keys.capture());
        return keys.getAllValues();
    }

    @Test
    @DisplayName("database sessions run in UTC so NOW() defaults match the application clock")
    void databaseSessionIsUtc() {
        assertThat(jdbcTemplate.queryForObject("SELECT current_setting('TimeZone')", String.class))
                .isIn("UTC", "Etc/UTC");
    }

    @Test
    @DisplayName("a rollback after a successful penalty charge keeps the charge recorded and the retry does not charge again")
    void rollbackAfterChargeDoesNotOrphanIt() {
        Rental rental = capturedRental();
        Payment penalty = penalty(rental, "25.00");

        assertThatThrownBy(() -> outer().executeWithoutResult(status -> {
            assertThat(paymentService.chargePayment(penalty.getId()).success()).isTrue();
            throw new IllegalStateException("caller failed after Stripe succeeded");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(paymentRepository.findById(penalty.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.CAPTURED);
        assertThat(paymentService.chargePayment(penalty.getId()).success()).isTrue();
        verify(paymentGateway, times(1)).chargeSavedMethod(any(ChargeCommand.class), anyString());
    }

    @Test
    @DisplayName("an ambiguous Stripe outcome keeps the penalty key so the retry is the same Stripe operation")
    void ambiguousOutcomeKeepsKey() {
        Rental rental = capturedRental();
        Payment penalty = penalty(rental, "25.00");
        doReturn(ChargeResult.ambiguous("connection reset"))
                .doCallRealMethod()
                .when(paymentGateway).chargeSavedMethod(any(ChargeCommand.class), anyString());

        assertThat(paymentService.chargePayment(penalty.getId()).success()).isFalse();
        assertThat(paymentRepository.findById(penalty.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.PENDING);
        assertThat(paymentService.chargePayment(penalty.getId()).success()).isTrue();

        List<String> keys = chargeKeys(2);
        assertThat(keys.get(0)).isEqualTo(keyPrefix() + "rental-" + rental.getId() + ":payment-" + penalty.getId() + ":penalty:1");
        assertThat(keys.get(1)).isEqualTo(keys.get(0));
    }

    @Test
    @DisplayName("a definitive card decline moves the penalty key to the next attempt")
    void declineBumpsKey() {
        Rental rental = capturedRental();
        Payment penalty = penalty(rental, "25.00");
        doReturn(ChargeResult.declined("Your card was declined."))
                .doCallRealMethod()
                .when(paymentGateway).chargeSavedMethod(any(ChargeCommand.class), anyString());

        assertThat(paymentService.chargePayment(penalty.getId()).success()).isFalse();
        assertThat(paymentService.chargePayment(penalty.getId()).success()).isTrue();

        List<String> keys = chargeKeys(2);
        assertThat(keys.get(0)).isEqualTo(keyPrefix() + "rental-" + rental.getId() + ":payment-" + penalty.getId() + ":penalty:1");
        assertThat(keys.get(1)).isEqualTo(keyPrefix() + "rental-" + rental.getId() + ":payment-" + penalty.getId() + ":penalty:2");
    }

    @Test
    @DisplayName("the penalty payment row is reused when the caller retries after a rollback")
    void penaltyRowIsReused() {
        Rental rental = capturedRental();

        Payment first = penalty(rental, "25.00");
        Payment second = penalty(rental, "25.00");

        assertThat(second.getId()).isEqualTo(first.getId());
    }

    @Test
    @DisplayName("damage charges key on the damage report and a rollback does not orphan the charge")
    void damageKeyAndRollback() {
        Rental rental = capturedRental();

        assertThatThrownBy(() -> outer().executeWithoutResult(status -> {
            assertThat(paymentService.chargeDamage(rental.getId(), 41L, new BigDecimal("90.00"), null).success())
                    .isTrue();
            throw new IllegalStateException("caller failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(paymentService.chargeDamage(rental.getId(), 41L, new BigDecimal("90.00"), null).success()).isTrue();
        assertThat(chargeKeys(1).get(0)).isEqualTo(keyPrefix() + "damage-41:charge:1");
    }

    @Test
    @DisplayName("a damage charge in another currency than the rental payment is rejected before Stripe")
    void damageCurrencyMismatchRejected() {
        Rental rental = capturedRental();

        PaymentResult result = paymentService.chargeDamage(rental.getId(), 42L, new BigDecimal("90"), CurrencyType.JPY);

        assertThat(result.success()).isFalse();
        verify(paymentGateway, never()).chargeSavedMethod(any(ChargeCommand.class), anyString());
    }

    @Test
    @DisplayName("a REFUNDED penalty payment is never charged again")
    void refundedPenaltyIsNotRecharged() {
        Rental rental = capturedRental();
        Payment penalty = penalty(rental, "25.00");
        jdbcTemplate.update("UPDATE car_rental.payments SET status = 'REFUNDED' WHERE id = ?", penalty.getId());

        PaymentResult result = paymentService.chargePayment(penalty.getId());

        assertThat(result.success()).isFalse();
        verify(paymentGateway, never()).chargeSavedMethod(any(ChargeCommand.class), anyString());
    }

    @Test
    @DisplayName("a refund recorded before the caller rolled back is not issued twice on retry")
    void refundRollbackThenRetry() {
        Rental rental = capturedRental();
        Payment payment = paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rental.getId(), PaymentKind.RENTAL)
                .orElseThrow();
        String op = "rental-" + rental.getId() + ":cancel-refund";
        BigDecimal part = new BigDecimal("100.00");

        assertThatThrownBy(() -> outer().executeWithoutResult(status -> {
            assertThat(paymentService.refundPayment(payment.getId(), part, op).success()).isTrue();
            throw new IllegalStateException("caller failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(paymentService.refundPayment(payment.getId(), part, op).success()).isTrue();

        verify(paymentGateway, times(1)).refund(anyString(), any(), any(), anyString());
        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getRefundedAmount())
                .isEqualByComparingTo(part);
    }

    @Test
    @DisplayName("when the local refund record is gone and the key has expired, only the missing difference is refunded")
    void refundAfterKeyExpiryUsesStripeTotal() {
        Rental rental = capturedRental();
        Payment payment = paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rental.getId(), PaymentKind.RENTAL)
                .orElseThrow();
        String op = "rental-" + rental.getId() + ":cancel-refund";
        BigDecimal part = new BigDecimal("100.00");
        assertThat(paymentService.refundPayment(payment.getId(), part, op).success()).isTrue();
        jdbcTemplate.update("DELETE FROM car_rental.payment_refunds");

        assertThat(paymentService.refundPayment(payment.getId(), part, op).success()).isTrue();

        verify(paymentGateway, times(1)).refund(anyString(), any(), any(), anyString());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM car_rental.payment_refunds WHERE status = 'SUCCEEDED'", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("concurrent refunds of one payment are serialised so the ledger, the payment and the gateway agree")
    void concurrentRefundsStayConsistent() throws Exception {
        Rental rental = capturedRental();
        Payment payment = paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rental.getId(), PaymentKind.RENTAL)
                .orElseThrow();
        String intentId = payment.getStripePaymentIntentId();
        CountDownLatch bothPlanned = new CountDownLatch(2);
        doAnswer(invocation -> {
            bothPlanned.countDown();
            bothPlanned.await(1500, java.util.concurrent.TimeUnit.MILLISECONDS);
            return invocation.callRealMethod();
        }).when(paymentGateway).refundedMinor(anyString());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<PaymentResult> first = pool.submit(
                    () -> paymentService.refundPayment(payment.getId(), new BigDecimal("100.00"), "waiver-1:refund"));
            Future<PaymentResult> second = pool.submit(
                    () -> paymentService.refundPayment(payment.getId(), new BigDecimal("100.00"), "waiver-2:refund"));

            assertThat(first.get(30, java.util.concurrent.TimeUnit.SECONDS).success()).isTrue();
            assertThat(second.get(30, java.util.concurrent.TimeUnit.SECONDS).success()).isTrue();
        } finally {
            pool.shutdownNow();
        }

        BigDecimal ledger = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM car_rental.payment_refunds WHERE status = 'SUCCEEDED'",
                BigDecimal.class);
        Integer ledgerRowsWithoutGatewayRefund = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM car_rental.payment_refunds WHERE status = 'SUCCEEDED' AND stripe_refund_id IS NULL",
                Integer.class);
        Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(ledger).isEqualByComparingTo("200.00");
        assertThat(ledgerRowsWithoutGatewayRefund).isZero();
        assertThat(reloaded.refundedTotal()).isEqualByComparingTo("200.00");
        assertThat(paymentGateway.refundedMinor(intentId)).isEqualTo(20000L);
    }

    @Test
    @DisplayName("a refund gives up with a retryable failure when the payment row stays locked past the lock timeout")
    void refundGivesUpWhenRowStaysLocked() throws Exception {
        Rental rental = capturedRental();
        Payment payment = paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rental.getId(), PaymentKind.RENTAL)
                .orElseThrow();
        Object target = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(paymentService);
        Object previous = org.springframework.test.util.ReflectionTestUtils.getField(target, "refundLockTimeoutMs");
        org.springframework.test.util.ReflectionTestUtils.setField(target, "refundLockTimeoutMs", 700L);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<?> holder = pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(s -> {
                jdbcTemplate.queryForList("SELECT id FROM car_rental.payments WHERE id = ? FOR UPDATE", payment.getId());
                locked.countDown();
                try {
                    release.await(20, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
            assertThat(locked.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

            long started = System.nanoTime();
            PaymentResult result = paymentService.refundPayment(payment.getId(), BigDecimal.TEN, "waiver-lock:refund");
            long elapsedMs = (System.nanoTime() - started) / 1_000_000;

            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("busy");
            assertThat(elapsedMs).isLessThan(10_000L);
            verify(paymentGateway, never()).refund(anyString(), any(), any(), anyString());
            release.countDown();
            holder.get(10, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            release.countDown();
            pool.shutdownNow();
            org.springframework.test.util.ReflectionTestUtils.setField(target, "refundLockTimeoutMs", previous);
        }
    }

    @Test
    @DisplayName("a payment closed by a rental cancellation is not restarted by a payment-intent call that raced it")
    void cancelledRentalPaymentIsNotRestarted() {
        Rental rental = rental(LocalDate.now().plusDays(2), LocalDate.now().plusDays(5), RentalStatus.CONFIRMED);
        Payment payment = rentalPayment(rental, PaymentStatus.PENDING);
        PaymentIntentResult first = paymentService.createOrGetIntentForRental(rental.getId());
        doAnswer(invocation -> {
            assertThat(paymentService.cancelPayment(payment.getId()).success()).isTrue();
            return new GatewayIntent(first.paymentIntentId(), null, GatewayIntentStatus.CANCELED,
                    null, "cus_race", 100L, "try");
        }).when(paymentGateway).retrieveIntent(first.paymentIntentId());

        assertThatThrownBy(() -> paymentService.createOrGetIntentForRental(rental.getId()))
                .isInstanceOf(com.akif.payment.api.InvalidPaymentStateException.class)
                .hasMessageContaining("closed");

        Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(reloaded.isClosed()).isTrue();
        verify(paymentGateway, times(1)).createIntent(any(IntentCommand.class), anyString());
    }

    @Test
    @DisplayName("a closed payment cannot be started even when its status is CANCELLED")
    void closedPaymentIsNotPayable() {
        Rental rental = rental(LocalDate.now().plusDays(2), LocalDate.now().plusDays(5), RentalStatus.CONFIRMED);
        Payment payment = rentalPayment(rental, PaymentStatus.PENDING);
        assertThat(paymentService.cancelPayment(payment.getId()).success()).isTrue();

        assertThatThrownBy(() -> paymentService.createOrGetIntentForRental(rental.getId()))
                .isInstanceOf(com.akif.payment.api.InvalidPaymentStateException.class);
        verify(paymentGateway, never()).createIntent(any(IntentCommand.class), anyString());
    }

    @Test
    @DisplayName("a payment cancelled remotely, not by its rental, can still be restarted")
    void remotelyCancelledPaymentStaysRestartable() {
        Rental rental = rental(LocalDate.now().plusDays(2), LocalDate.now().plusDays(5), RentalStatus.CONFIRMED);
        Payment payment = rentalPayment(rental, PaymentStatus.PENDING);
        PaymentIntentResult first = paymentService.createOrGetIntentForRental(rental.getId());
        paymentGateway.cancel(first.paymentIntentId(), "remote-cancel");
        jdbcTemplate.update("UPDATE car_rental.payments SET status = 'CANCELLED' WHERE id = ?", payment.getId());

        PaymentIntentResult restarted = paymentService.createOrGetIntentForRental(rental.getId());

        assertThat(restarted.paymentIntentId()).isNotEqualTo(first.paymentIntentId());
        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().isClosed()).isFalse();
    }

    @Test
    @DisplayName("a failed refund keeps its key for the retry and is recorded once it succeeds")
    void failedRefundThenRetry() {
        Rental rental = capturedRental();
        Payment payment = paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rental.getId(), PaymentKind.RENTAL)
                .orElseThrow();
        String op = "waiver-9:refund";
        doReturn(PaymentResult.failure("network"))
                .doCallRealMethod()
                .when(paymentGateway).refund(anyString(), any(), any(), anyString());

        assertThat(paymentService.refundPayment(payment.getId(), BigDecimal.TEN, op).success()).isFalse();
        assertThat(paymentService.refundPayment(payment.getId(), BigDecimal.TEN, op).success()).isTrue();

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(paymentGateway, times(2)).refund(anyString(), any(), any(), keys.capture());
        assertThat(keys.getAllValues().get(0)).isEqualTo(keys.getAllValues().get(1));
        assertThat(keys.getAllValues().get(0)).isEqualTo(keyPrefix() + "waiver-9:refund");
    }

    @Test
    @DisplayName("parallel payment-intent calls after a cancellation leave exactly one tracked intent and every caller gets its secret")
    void parallelIntentsAfterCancellation() throws Exception {
        Rental rental = rental(LocalDate.now().plusDays(2), LocalDate.now().plusDays(5), RentalStatus.CONFIRMED);
        Payment payment = rentalPayment(rental, PaymentStatus.PENDING);
        PaymentIntentResult first = paymentService.createOrGetIntentForRental(rental.getId());
        paymentGateway.cancel(first.paymentIntentId(), "test-cancel");
        jdbcTemplate.update("UPDATE car_rental.payments SET status = 'PENDING' WHERE id = ?", payment.getId());

        AtomicInteger sequence = new AtomicInteger();
        CountDownLatch bothCreated = new CountDownLatch(2);
        doAnswer(invocation -> {
            int n = sequence.incrementAndGet();
            bothCreated.countDown();
            bothCreated.await(5, java.util.concurrent.TimeUnit.SECONDS);
            IntentCommand command = invocation.getArgument(0);
            return new GatewayIntent("pi_race_" + n, "secret_race_" + n, GatewayIntentStatus.REQUIRES_PAYMENT_METHOD,
                    null, "cus_race", 100L, "try");
        }).when(paymentGateway).createIntent(any(IntentCommand.class), anyString());
        doReturn(new GatewayIntent("pi_race_1", "secret_race_1", GatewayIntentStatus.REQUIRES_PAYMENT_METHOD,
                null, "cus_race", 100L, "try")).when(paymentGateway).retrieveIntent("pi_race_1");
        doReturn(new GatewayIntent("pi_race_2", "secret_race_2", GatewayIntentStatus.REQUIRES_PAYMENT_METHOD,
                null, "cus_race", 100L, "try")).when(paymentGateway).retrieveIntent("pi_race_2");
        doReturn(PaymentResult.success("x", "cancelled")).when(paymentGateway).cancel(anyString(), anyString());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<PaymentIntentResult>> calls = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                calls.add(pool.submit(() -> paymentService.createOrGetIntentForRental(rental.getId())));
            }
            List<PaymentIntentResult> results = new ArrayList<>();
            for (Future<PaymentIntentResult> call : calls) {
                results.add(call.get(20, java.util.concurrent.TimeUnit.SECONDS));
            }

            String tracked = paymentRepository.findById(payment.getId()).orElseThrow().getStripePaymentIntentId();
            assertThat(results).extracting(PaymentIntentResult::paymentIntentId).containsOnly(tracked);
            assertThat(results).extracting(PaymentIntentResult::clientSecret).doesNotContainNull();
            assertThat(results.stream().map(PaymentIntentResult::clientSecret).distinct()).hasSize(1);
            verify(paymentGateway, times(1)).cancel(
                    org.mockito.ArgumentMatchers.argThat(id -> id.startsWith("pi_race_") && !id.equals(tracked)),
                    anyString());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("a CONFIRMED rental with no authorized payment is cancelled when its start date arrives")
    void unpaidRentalExpiresOnStartDate() {
        Rental rental = rental(LocalDate.now(), LocalDate.now().plusDays(3), RentalStatus.CONFIRMED);
        Payment payment = rentalPayment(rental, PaymentStatus.PENDING);

        int expired = expiryScheduler.expireUnpaidRentals();

        assertThat(expired).isEqualTo(1);
        assertThat(rentalRepository.findById(rental.getId()).orElseThrow().getStatus()).isEqualTo(RentalStatus.CANCELLED);
        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        List<RentalCancelledEvent> events = eventCaptor.getEventsOfType(RentalCancelledEvent.class);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).cancellationReason()).contains("Payment not completed before the start date");
    }

    @Test
    @DisplayName("an unpaid rental releases its open hold when it expires")
    void expiryReleasesOpenIntent() {
        Rental rental = rental(LocalDate.now(), LocalDate.now().plusDays(3), RentalStatus.CONFIRMED);
        rentalPayment(rental, PaymentStatus.PENDING);
        PaymentIntentResult intent = paymentService.createOrGetIntentForRental(rental.getId());
        jdbcTemplate.update("UPDATE car_rental.payments SET status = 'PENDING' WHERE rental_id = ?", rental.getId());

        expiryScheduler.expireUnpaidRentals();

        verify(paymentGateway).cancel(org.mockito.ArgumentMatchers.eq(intent.paymentIntentId()), anyString());
    }

    @Test
    @DisplayName("the expiry scheduler leaves authorized, captured and future rentals alone")
    void expirySkipsPaidAndFutureRentals() {
        Rental authorized = rental(LocalDate.now(), LocalDate.now().plusDays(3), RentalStatus.CONFIRMED);
        rentalPayment(authorized, PaymentStatus.PENDING);
        paymentService.createOrGetIntentForRental(authorized.getId());
        Car other = carRepository.save(TestDataBuilder.createAvailableCar("34ZZ9999", new BigDecimal("100.00")));
        Rental future = rentalRepository.save(TestDataBuilder.createRental(
                other, customer, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4), RentalStatus.CONFIRMED));
        rentalPayment(future, PaymentStatus.PENDING);

        int expired = expiryScheduler.expireUnpaidRentals();

        assertThat(expired).isZero();
        assertThat(rentalRepository.findById(authorized.getId()).orElseThrow().getStatus()).isEqualTo(RentalStatus.CONFIRMED);
        assertThat(rentalRepository.findById(future.getId()).orElseThrow().getStatus()).isEqualTo(RentalStatus.CONFIRMED);
    }

    @Test
    @DisplayName("a cancelled payment inside the window asks the customer to pay again")
    void repayEventInsideWindow() {
        rentalProperties.setPaymentWindowDays(5);
        Rental rental = rental(LocalDate.now().plusDays(3), LocalDate.now().plusDays(6), RentalStatus.CONFIRMED);

        paymentCancelledListener.onPaymentCancelled(new PaymentCancelledEvent(
                1L, rental.getId(), rental.getUserEmail(), new BigDecimal("10.00"), CurrencyType.TRY,
                java.time.Instant.now()));

        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(
                () -> assertThat(eventCaptor.getEventsOfType(PaymentRetryRequestedEvent.class)).hasSize(1));
        List<PaymentRetryRequestedEvent> events = eventCaptor.getEventsOfType(PaymentRetryRequestedEvent.class);
        assertThat(events.get(0).rentalId()).isEqualTo(rental.getId());
        assertThat(events.get(0).payBy()).isEqualTo(rental.getStartDate());
    }

    @Test
    @DisplayName("no pay-again event is sent before the window opens or once the rental is no longer CONFIRMED")
    void noRepayEventOutsideWindow() {
        rentalProperties.setPaymentWindowDays(5);
        Rental early = rental(LocalDate.now().plusDays(20), LocalDate.now().plusDays(22), RentalStatus.CONFIRMED);
        Car other = carRepository.save(TestDataBuilder.createAvailableCar("34YY8888", new BigDecimal("100.00")));
        Rental cancelled = rentalRepository.save(TestDataBuilder.createRental(
                other, customer, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4), RentalStatus.CANCELLED));

        for (Rental target : List.of(early, cancelled)) {
            paymentCancelledListener.onPaymentCancelled(new PaymentCancelledEvent(
                    1L, target.getId(), target.getUserEmail(), BigDecimal.TEN, CurrencyType.TRY,
                    java.time.Instant.now()));
        }

        org.awaitility.Awaitility.await().during(java.time.Duration.ofSeconds(2)).atMost(java.time.Duration.ofSeconds(6))
                .untilAsserted(() -> assertThat(eventCaptor.getEventsOfType(PaymentRetryRequestedEvent.class)).isEmpty());
    }

    @Test
    @DisplayName("concurrent cancel and pickup have exactly one winner and a consistent payment state")
    void cancelVersusPickup() throws Exception {
        Rental rental = rental(LocalDate.now().plusDays(1), LocalDate.now().plusDays(4), RentalStatus.CONFIRMED);
        rentalPayment(rental, PaymentStatus.PENDING);
        paymentService.createOrGetIntentForRental(rental.getId());
        String username = customer.getUsername();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            Future<Boolean> cancel = pool.submit(() -> attempt(go, () -> rentalService.cancelRental(rental.getId(), username, "changed")));
            Future<Boolean> pickup = pool.submit(() -> attempt(go, () -> rentalService.pickupRental(rental.getId(), "go")));
            go.countDown();
            boolean cancelled = cancel.get(30, java.util.concurrent.TimeUnit.SECONDS);
            boolean pickedUp = pickup.get(30, java.util.concurrent.TimeUnit.SECONDS);

            assertThat(cancelled ^ pickedUp).isTrue();
            Rental result = rentalRepository.findById(rental.getId()).orElseThrow();
            PaymentStatus paymentStatus = paymentRepository
                    .findByRentalIdAndKindAndIsDeletedFalse(rental.getId(), PaymentKind.RENTAL).orElseThrow().getStatus();
            if (cancelled) {
                assertThat(result.getStatus()).isEqualTo(RentalStatus.CANCELLED);
                assertThat(paymentStatus).isEqualTo(PaymentStatus.CANCELLED);
            } else {
                assertThat(result.getStatus()).isEqualTo(RentalStatus.IN_USE);
                assertThat(paymentStatus).isEqualTo(PaymentStatus.CAPTURED);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private boolean attempt(CountDownLatch go, Runnable action) throws InterruptedException {
        go.await();
        try {
            action.run();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Test
    @DisplayName("a partial refund in a zero-decimal currency is rounded down to whole units")
    void jpyPartialRefundRoundsDown() {
        Rental rental = rental(LocalDate.now().minusDays(1), LocalDate.now().plusDays(2), RentalStatus.IN_USE);
        jdbcTemplate.update("UPDATE car_rental.rentals SET currency = 'JPY', total_price = 10000, days = 4 WHERE id = ?",
                rental.getId());
        jdbcTemplate.update("UPDATE car_rental.car SET currency_type = 'JPY' WHERE id = ?", car.getId());
        PaymentDto dto = paymentService.createPayment(new CreatePaymentRequest(
                rental.getId(), rental.getUserEmail(), rental.getCarLicensePlate(),
                new BigDecimal("10000"), CurrencyType.JPY, "CARD"));
        Payment payment = paymentRepository.findById(dto.id()).orElseThrow();
        payment.attachIntent("pi_jpy", "seed");
        payment.capture();
        paymentRepository.saveAndFlush(payment);
        BigDecimal wholeStay = dynamicPricingService.calculatePrice(
                car.getId(), rental.getStartDate(), rental.getEndDate(), LocalDate.now(), rental.getId()).finalPrice();
        BigDecimal usedDaysAtCurrentPrice = dynamicPricingService.calculatePrice(
                car.getId(), rental.getStartDate(), rental.getStartDate().plusDays(1), LocalDate.now(), rental.getId()).finalPrice();
        BigDecimal usedDays = usedDaysAtCurrentPrice
                .multiply(new BigDecimal("10000").divide(wholeStay, 8, java.math.RoundingMode.HALF_UP))
                .setScale(0, java.math.RoundingMode.HALF_UP);

        rentalService.earlyReturnRental(rental.getId(), "returned early");

        ArgumentCaptor<BigDecimal> amount = ArgumentCaptor.forClass(BigDecimal.class);
        verify(paymentGateway).refund(anyString(), amount.capture(), org.mockito.ArgumentMatchers.eq(CurrencyType.JPY), anyString());
        assertThat(amount.getValue()).isEqualByComparingTo(
                new BigDecimal("10000").subtract(usedDays).setScale(0, java.math.RoundingMode.DOWN));
        assertThat(amount.getValue().scale()).isZero();
    }
}
