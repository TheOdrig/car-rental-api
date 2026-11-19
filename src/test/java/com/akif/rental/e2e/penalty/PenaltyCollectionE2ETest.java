package com.akif.rental.e2e.penalty;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.dashboard.api.AlertType;
import com.akif.dashboard.domain.model.Alert;
import com.akif.dashboard.internal.repository.AlertRepository;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.e2e.infrastructure.TestFixtures;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.Payment;
import com.akif.payment.domain.enums.PaymentKind;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.internal.service.PaymentStateService;
import com.akif.payment.internal.service.reconciliation.PaymentRepairService;
import com.akif.payment.internal.service.gateway.ChargeResult;
import com.akif.payment.internal.service.gateway.GatewayIntent;
import com.akif.payment.internal.service.gateway.GatewayIntentStatus;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import com.akif.rental.api.PenaltyChargeFailedEvent;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.enums.LateReturnStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.dto.penalty.PenaltyWaiverRequest;
import com.akif.rental.internal.dto.request.ReturnRequest;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@DisplayName("Penalty collection E2E Tests")
class PenaltyCollectionE2ETest extends E2ETestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private PaymentStateService paymentStateService;

    @Autowired
    private PaymentRepairService repairService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @MockitoSpyBean
    private PaymentGateway paymentGateway;

    private User admin;
    private Rental rental;

    private void givenOverdueRental(int daysOverdue) {
        User user = userRepository.save(TestDataBuilder.createTestUser());
        admin = userRepository.save(TestDataBuilder.createTestAdmin());
        Car car = carRepository.save(TestDataBuilder.createAvailableCar());
        LocalDate today = LocalDate.now(clock);
        rental = rentalRepository.save(Rental.builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userFullName(user.getUsername())
                .carId(car.getId())
                .carBrand(car.getBrand())
                .carModel(car.getModel())
                .carLicensePlate(car.getLicensePlate())
                .startDate(today.minusDays(daysOverdue + 3))
                .endDate(today.minusDays(daysOverdue))
                .days(4)
                .currency(TestFixtures.DEFAULT_CURRENCY)
                .dailyPrice(new BigDecimal("500.00"))
                .totalPrice(new BigDecimal("2000.00"))
                .status(RentalStatus.IN_USE)
                .penaltyPaid(false)
                .isDeleted(false)
                .build());
        paymentRepository.save(Payment.builder()
                .rentalId(rental.getId())
                .kind(PaymentKind.RENTAL)
                .userEmail(rental.getUserEmail())
                .carLicensePlate(rental.getCarLicensePlate())
                .amount(rental.getTotalPrice())
                .currency(rental.getCurrency())
                .status(PaymentStatus.CAPTURED)
                .paymentMethod("CARD")
                .stripePaymentIntentId("stub_pi_seed_" + rental.getId())
                .transactionId("stub_pi_seed_" + rental.getId())
                .stripeCustomerId("cus_test")
                .stripePaymentMethodId("pm_test")
                .isDeleted(false)
                .build());
    }

    private void returnRental() throws Exception {
        mockMvc.perform(post("/api/v1/rentals/{id}/return", rental.getId())
                        .header("Authorization", "Bearer " + generateAdminToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReturnRequest("returned"))))
                .andExpect(status().isOk());
    }

    private void waive(BigDecimal amount) throws Exception {
        mockMvc.perform(post("/api/v1/admin/rentals/{id}/penalty/waive", rental.getId())
                        .header("Authorization", "Bearer " + generateAdminToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PenaltyWaiverRequest(amount, "Goodwill gesture for the customer", null))))
                .andExpect(status().isOk());
    }

    private Rental reload() {
        return rentalRepository.findById(rental.getId()).orElseThrow();
    }

    private Payment penaltyPayment() {
        return paymentRepository.findFirstByRentalIdAndKindAndIsDeletedFalseOrderByIdDesc(rental.getId(), PaymentKind.PENALTY)
                .orElseThrow();
    }

    private void captureOutsideTheReturn(Payment payment, String intentId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> paymentStateService.markCaptured(
                paymentRepository.findById(payment.getId()).orElseThrow(), intentId));
    }

    @Test
    @DisplayName("a late return is penalised from the actual return time even when the scheduler never flagged it")
    void returnPenalisesWithoutTheSchedulerFlag() throws Exception {
        givenOverdueRental(2);

        returnRental();

        Rental returned = reload();
        assertThat(returned.getPenaltyAmount()).isEqualByComparingTo("1500.00");
        assertThat(returned.getPenaltyPaid()).isTrue();
        assertThat(returned.getLateReturnStatus()).isEqualTo(LateReturnStatus.SEVERELY_LATE);
        assertThat(penaltyPayment().getStatus()).isEqualTo(PaymentStatus.CAPTURED);
    }

    @Test
    @DisplayName("a stale scheduler flag is replaced by the status computed at return")
    void returnReplacesAStaleFlag() throws Exception {
        givenOverdueRental(2);
        jdbcTemplate.update("UPDATE car_rental.rentals SET late_return_status = 'GRACE_PERIOD' WHERE id = ?", rental.getId());

        returnRental();

        assertThat(reload().getLateReturnStatus()).isEqualTo(LateReturnStatus.SEVERELY_LATE);
    }

    @Test
    @DisplayName("the penalty charge key carries the payment id")
    void chargeKeyCarriesThePaymentId() throws Exception {
        givenOverdueRental(2);

        returnRental();

        verify(paymentGateway).chargeSavedMethod(any(), contains(":payment-" + penaltyPayment().getId() + ":penalty:"));
    }

    @Test
    @DisplayName("a retried return does not charge a second penalty when one was already captured")
    void alreadyCollectedPenaltyIsNotChargedTwice() throws Exception {
        givenOverdueRental(2);
        paymentRepository.save(Payment.builder()
                .rentalId(rental.getId())
                .kind(PaymentKind.PENALTY)
                .userEmail(rental.getUserEmail())
                .carLicensePlate(rental.getCarLicensePlate())
                .amount(new BigDecimal("1000.00"))
                .currency(rental.getCurrency())
                .status(PaymentStatus.CAPTURED)
                .paymentMethod("PENALTY")
                .stripePaymentIntentId("stub_pi_penalty_seed")
                .transactionId("stub_pi_penalty_seed")
                .isDeleted(false)
                .build());

        returnRental();

        verify(paymentGateway, never()).chargeSavedMethod(any(), anyString());
        Rental returned = reload();
        assertThat(returned.getPenaltyPaid()).isTrue();
        assertThat(returned.getPenaltyAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("a penalty confirmed after the return by the gateway is recorded as paid and a waiver refunds it")
    void lateConfirmedPenaltyIsPaidAndRefundedOnWaiver() throws Exception {
        givenOverdueRental(2);
        doReturn(ChargeResult.ambiguous("Gateway timeout")).when(paymentGateway).chargeSavedMethod(any(), anyString());

        returnRental();

        assertThat(reload().getPenaltyPaid()).isFalse();
        Payment pending = penaltyPayment();
        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.PENDING);

        captureOutsideTheReturn(pending, "pi_late_confirmed");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(reload().getPenaltyPaid()).isTrue());

        waive(reload().getPenaltyAmount());

        verify(paymentGateway).refund(anyString(), any(BigDecimal.class), any(), anyString());
        assertThat(paymentRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(reload().getPenaltyPaid()).isFalse();
    }

    @Test
    @DisplayName("a capture arriving after a waiver closed the payment is refunded automatically")
    void lateCaptureAfterWaiverIsRefunded() throws Exception {
        givenOverdueRental(2);
        doReturn(ChargeResult.ambiguous("Gateway timeout")).when(paymentGateway).chargeSavedMethod(any(), anyString());
        returnRental();
        Payment pending = penaltyPayment();

        waive(reload().getPenaltyAmount());

        Payment closed = paymentRepository.findById(pending.getId()).orElseThrow();
        assertThat(closed.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(closed.isClosed()).isTrue();
        verify(paymentGateway, never()).refund(anyString(), any(BigDecimal.class), any(), anyString());

        captureOutsideTheReturn(pending, "pi_after_waiver");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(paymentRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.REFUNDED));
        verify(paymentGateway).refund(eq("pi_after_waiver"), any(BigDecimal.class), any(), anyString());
        assertThat(reload().getPenaltyPaid()).isFalse();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(alertRepository
                .findByTypeAndAcknowledgedFalse(AlertType.FAILED_PAYMENT)).anySatisfy(alert -> {
            assertThat(alert.getReferenceId()).isEqualTo(rental.getId());
            assertThat(alert.getTitle()).isEqualTo("Charge captured after the payment was closed");
        }));
    }

    @Test
    @DisplayName("a waiver finds a charge the gateway did complete and refunds it instead of closing the payment")
    void waiverResolvesAnAmbiguousChargeThatSucceeded() throws Exception {
        givenOverdueRental(2);
        doReturn(ChargeResult.ambiguous("Gateway timeout")).when(paymentGateway).chargeSavedMethod(any(), anyString());
        returnRental();
        Payment pending = penaltyPayment();
        doReturn(Optional.of(new GatewayIntent("pi_resolved", null, GatewayIntentStatus.SUCCEEDED, "pm_test", "cus_test",
                150000L, "try"))).when(paymentGateway).findIntentByPayment(pending.getId());

        waive(reload().getPenaltyAmount());

        Payment resolved = paymentRepository.findById(pending.getId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(resolved.getStripePaymentIntentId()).isEqualTo("pi_resolved");
        verify(paymentGateway).refund(eq("pi_resolved"), any(BigDecimal.class), any(), anyString());
    }

    @Test
    @DisplayName("an ambiguous charge raises the unknown-outcome alert and not the could-not-be-charged one")
    void ambiguousChargeRaisesTheUnknownOutcomeAlert() throws Exception {
        givenOverdueRental(2);
        doReturn(ChargeResult.ambiguous("Gateway timeout")).when(paymentGateway).chargeSavedMethod(any(), anyString());

        returnRental();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(alertRepository
                .findByTypeAndAcknowledgedFalse(AlertType.FAILED_PAYMENT)).anySatisfy(alert -> {
            assertThat(alert.getReferenceId()).isEqualTo(rental.getId());
            assertThat(alert.getTitle()).contains("do not collect manually");
        }));
        assertThat(alertRepository.findByTypeAndAcknowledgedFalse(AlertType.FAILED_PAYMENT))
                .noneSatisfy(alert -> assertThat(alert.getMessage()).contains("could not be charged"));
        assertThat(penaltyPayment().hasUnknownOutcome()).isTrue();
    }

    @Test
    @DisplayName("a partial waiver of an unpaid penalty collects the remainder with a new payment")
    void partialWaiverOfAnUnpaidPenaltyCollectsTheRemainder() throws Exception {
        givenOverdueRental(2);
        doReturn(ChargeResult.ambiguous("Gateway timeout")).doCallRealMethod()
                .when(paymentGateway).chargeSavedMethod(any(), anyString());
        returnRental();
        Payment first = penaltyPayment();

        waive(new BigDecimal("500.00"));

        Payment old = paymentRepository.findById(first.getId()).orElseThrow();
        assertThat(old.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(old.isClosed()).isTrue();
        Payment remainder = penaltyPayment();
        assertThat(remainder.getId()).isNotEqualTo(first.getId());
        assertThat(remainder.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(remainder.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(reload().getPenaltyAmount()).isEqualByComparingTo("1000.00");
        assertThat(reload().getPenaltyPaid()).isTrue();
    }

    @Test
    @DisplayName("a second penalty captured after a rolled-back return is refunded as a duplicate")
    void duplicatePenaltyCaptureIsRefunded() throws Exception {
        givenOverdueRental(2);
        Payment collected = paymentRepository.save(Payment.builder()
                .rentalId(rental.getId())
                .kind(PaymentKind.PENALTY)
                .userEmail(rental.getUserEmail())
                .carLicensePlate(rental.getCarLicensePlate())
                .amount(new BigDecimal("1500.00"))
                .currency(rental.getCurrency())
                .status(PaymentStatus.CAPTURED)
                .paymentMethod("PENALTY")
                .stripePaymentIntentId("stub_pi_first_penalty")
                .transactionId("stub_pi_first_penalty")
                .isDeleted(false)
                .build());
        Payment duplicate = paymentRepository.save(Payment.builder()
                .rentalId(rental.getId())
                .kind(PaymentKind.PENALTY)
                .userEmail(rental.getUserEmail())
                .carLicensePlate(rental.getCarLicensePlate())
                .amount(new BigDecimal("1500.00"))
                .currency(rental.getCurrency())
                .status(PaymentStatus.PENDING)
                .paymentMethod("PENALTY")
                .isDeleted(false)
                .build());

        captureOutsideTheReturn(duplicate, "pi_duplicate_penalty");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(paymentRepository.findById(duplicate.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.REFUNDED));
        assertThat(paymentRepository.findById(collected.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        verify(paymentGateway).refund(eq("pi_duplicate_penalty"), any(BigDecimal.class), any(), anyString());
    }

    @Test
    @DisplayName("the repair sweep finds a penalty charge that completed at the gateway without an intent id on file")
    void repairResolvesAPenaltyWithoutAnIntentId() throws Exception {
        givenOverdueRental(2);
        doReturn(ChargeResult.ambiguous("Gateway timeout")).when(paymentGateway).chargeSavedMethod(any(), anyString());
        returnRental();
        Payment pending = penaltyPayment();
        assertThat(pending.getStripePaymentIntentId()).isNull();
        makeStale(pending);
        doReturn(Optional.of(new GatewayIntent("pi_found", null, GatewayIntentStatus.SUCCEEDED, "pm_test", "cus_test",
                150000L, "try"))).when(paymentGateway).findIntentByPayment(pending.getId());

        assertThat(repairService.repairStalePayments()).isEqualTo(1);

        Payment repaired = paymentRepository.findById(pending.getId()).orElseThrow();
        assertThat(repaired.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(repaired.getStripePaymentIntentId()).isEqualTo("pi_found");
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(reload().getPenaltyPaid()).isTrue());
    }

    @Test
    @DisplayName("the repair sweep refunds a closed penalty capture whose automatic refund failed")
    void repairRetriesAFailedAutomaticRefund() throws Exception {
        givenOverdueRental(2);
        doReturn(ChargeResult.ambiguous("Gateway timeout")).when(paymentGateway).chargeSavedMethod(any(), anyString());
        returnRental();
        Payment pending = penaltyPayment();
        waive(reload().getPenaltyAmount());
        doReturn(PaymentResult.failure("Stripe unavailable")).doCallRealMethod()
                .when(paymentGateway).refund(anyString(), any(BigDecimal.class), any(), anyString());

        captureOutsideTheReturn(pending, "pi_retry_refund");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(alertRepository
                .findByTypeAndAcknowledgedFalse(AlertType.FAILED_PAYMENT))
                .anySatisfy(alert -> assertThat(alert.getTitle()).isEqualTo("Automatic refund failed")));
        assertThat(paymentRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        makeStale(pending);

        repairService.repairStalePayments();

        assertThat(paymentRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    private void makeStale(Payment payment) {
        jdbcTemplate.update("UPDATE car_rental.payments SET update_time = ?, last_synced_at = NULL WHERE id = ?",
                java.sql.Timestamp.from(java.time.Instant.now(clock).minus(Duration.ofHours(2))), payment.getId());
    }

    @Test
    @DisplayName("a declined penalty charge raises an admin alert for the rental")
    void declinedPenaltyChargeRaisesAnAlert() throws Exception {
        givenOverdueRental(2);
        doReturn(ChargeResult.declined("Your card was declined")).when(paymentGateway).chargeSavedMethod(any(), anyString());

        returnRental();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findByTypeAndAcknowledgedFalse(AlertType.FAILED_PAYMENT);
            assertThat(alerts).anySatisfy(alert -> {
                assertThat(alert.getReferenceId()).isEqualTo(rental.getId());
                assertThat(alert.getMessage()).contains("1500.00").contains("declined");
            });
        });
        assertThat(reload().getPenaltyPaid()).isFalse();
    }
    private void awaitPenaltyAlert(Long paymentId, String messagePart) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(alertRepository
                .findByTypeAndAcknowledgedFalse(AlertType.FAILED_PAYMENT)).anySatisfy(alert -> {
            assertThat(alert.getReferenceId()).isEqualTo(rental.getId());
            assertThat(alert.getTitle()).isEqualTo("Late return penalty not collected");
            assertThat(alert.getMessage()).contains(messagePart).contains("(payment #" + paymentId + ")");
        }));
    }

    @Test
    @DisplayName("an unknown-outcome penalty that never reached the gateway is failed with an alert once it is stale")
    void unknownOutcomeThatNeverReachedTheGatewayIsFailedAndAlerted() throws Exception {
        givenOverdueRental(2);
        doReturn(ChargeResult.ambiguous("Gateway timeout")).when(paymentGateway).chargeSavedMethod(any(), anyString());
        returnRental();
        Payment pending = penaltyPayment();
        assertThat(pending.hasUnknownOutcome()).isTrue();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(alertRepository
                .findByTypeAndAcknowledgedFalse(AlertType.FAILED_PAYMENT))
                .anySatisfy(alert -> assertThat(alert.getTitle()).contains("do not collect manually")));
        makeStale(pending);

        assertThat(repairService.repairStalePayments()).isEqualTo(1);

        Payment failed = paymentRepository.findById(pending.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failed.hasUnknownOutcome()).isFalse();
        assertThat(failed.isClosed()).isFalse();
        awaitPenaltyAlert(pending.getId(), "never completed");
        assertThat(reload().getPenaltyPaid()).isFalse();
    }

    @Test
    @DisplayName("an unknown-outcome penalty whose intent the gateway shows as declined is failed with an alert")
    void unknownOutcomeThatWasDeclinedIsFailedAndAlerted() throws Exception {
        givenOverdueRental(2);
        doReturn(ChargeResult.ambiguous("Gateway timeout")).when(paymentGateway).chargeSavedMethod(any(), anyString());
        returnRental();
        Payment pending = penaltyPayment();
        makeStale(pending);
        doReturn(Optional.of(new GatewayIntent("pi_declined", null, GatewayIntentStatus.REQUIRES_PAYMENT_METHOD, "pm_test",
                "cus_test", 150000L, "try"))).when(paymentGateway).findIntentByPayment(pending.getId());

        assertThat(repairService.repairStalePayments()).isEqualTo(1);

        assertThat(paymentRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.FAILED);
        awaitPenaltyAlert(pending.getId(), "never completed");
    }

    @Test
    @DisplayName("an unknown-outcome penalty still in flight at the gateway is left alone")
    void unknownOutcomeStillInFlightIsLeftAlone() throws Exception {
        givenOverdueRental(2);
        doReturn(ChargeResult.ambiguous("Gateway timeout")).when(paymentGateway).chargeSavedMethod(any(), anyString());
        returnRental();
        Payment pending = penaltyPayment();
        makeStale(pending);
        doReturn(Optional.of(new GatewayIntent("pi_processing", null, GatewayIntentStatus.PROCESSING, "pm_test",
                "cus_test", 150000L, "try"))).when(paymentGateway).findIntentByPayment(pending.getId());

        assertThat(repairService.repairStalePayments()).isZero();

        assertThat(paymentRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("two uncollected penalty payments of one rental raise two alerts")
    void alertsAreDeduplicatedPerPayment() throws Exception {
        givenOverdueRental(2);
        PenaltyChargeFailedEvent first = new PenaltyChargeFailedEvent(rental.getId(), 9001L, rental.getUserEmail(),
                java.time.Instant.now(clock), new BigDecimal("10.00"), rental.getCurrency(), "declined");
        PenaltyChargeFailedEvent second = new PenaltyChargeFailedEvent(rental.getId(), 9002L, rental.getUserEmail(),
                java.time.Instant.now(clock), new BigDecimal("10.00"), rental.getCurrency(), "declined");

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(first);
            eventPublisher.publishEvent(second);
            eventPublisher.publishEvent(first);
        });

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(alertRepository
                .findByTypeAndAcknowledgedFalse(AlertType.FAILED_PAYMENT))
                .filteredOn(alert -> rental.getId().equals(alert.getReferenceId()))
                .extracting(Alert::getMessage)
                .hasSize(2)
                .anyMatch(message -> message.contains("(payment #9001)"))
                .anyMatch(message -> message.contains("(payment #9002)")));
    }

    @Test
    @DisplayName("a closed late capture awaiting its refund does not make the remainder charge look like a duplicate")
    void closedLateCaptureDoesNotMakeTheRemainderADuplicate() throws Exception {
        givenOverdueRental(2);
        doReturn(ChargeResult.ambiguous("Gateway timeout")).when(paymentGateway).chargeSavedMethod(any(), anyString());
        returnRental();
        Payment first = penaltyPayment();
        waive(new BigDecimal("500.00"));
        Payment remainder = penaltyPayment();
        assertThat(remainder.getId()).isNotEqualTo(first.getId());
        assertThat(remainder.getStatus()).isEqualTo(PaymentStatus.PENDING);
        doReturn(PaymentResult.failure("Stripe unavailable")).when(paymentGateway)
                .refund(anyString(), any(BigDecimal.class), any(), anyString());

        captureOutsideTheReturn(first, "pi_late_first");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(alertRepository
                .findByTypeAndAcknowledgedFalse(AlertType.FAILED_PAYMENT))
                .anySatisfy(alert -> assertThat(alert.getTitle()).isEqualTo("Automatic refund failed")));
        Payment lateFirst = paymentRepository.findById(first.getId()).orElseThrow();
        assertThat(lateFirst.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(lateFirst.isClosed()).isTrue();

        captureOutsideTheReturn(remainder, "pi_remainder");

        Payment kept = paymentRepository.findById(remainder.getId()).orElseThrow();
        assertThat(kept.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(kept.isClosed()).isFalse();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(reload().getPenaltyPaid()).isTrue());

        doCallRealMethod().when(paymentGateway).refund(anyString(), any(BigDecimal.class), any(), anyString());
        makeStale(first);
        repairService.repairStalePayments();

        assertThat(paymentRepository.findById(first.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(paymentRepository.findById(remainder.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        verify(paymentGateway, never()).refund(eq("pi_remainder"), any(BigDecimal.class), any(), anyString());
        assertThat(reload().getPenaltyPaid()).isTrue();
    }

    @Test
    @DisplayName("a remainder charge that blows up keeps the waiver, leaves the remainder open and raises an alert")
    void remainderChargeFailureDoesNotRollBackTheWaiver() throws Exception {
        givenOverdueRental(2);
        doReturn(ChargeResult.ambiguous("Gateway timeout")).doThrow(new IllegalStateException("gateway down"))
                .when(paymentGateway).chargeSavedMethod(any(), anyString());
        returnRental();
        Payment first = penaltyPayment();

        waive(new BigDecimal("500.00"));

        Integer waivers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM car_rental.penalty_waivers WHERE rental_id = ?", Integer.class, rental.getId());
        assertThat(waivers).isEqualTo(1);
        assertThat(reload().getPenaltyAmount()).isEqualByComparingTo("1000.00");
        Payment old = paymentRepository.findById(first.getId()).orElseThrow();
        assertThat(old.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(old.isClosed()).isTrue();
        Payment remainder = penaltyPayment();
        assertThat(remainder.getId()).isNotEqualTo(first.getId());
        assertThat(remainder.getStatus()).isEqualTo(PaymentStatus.PENDING);
        awaitPenaltyAlert(remainder.getId(), "gateway down");
    }

    @Test
    @DisplayName("a declined remainder charge after a waiver raises an alert for the remainder payment")
    void declinedRemainderRaisesAnAlert() throws Exception {
        givenOverdueRental(2);
        doReturn(ChargeResult.ambiguous("Gateway timeout")).doReturn(ChargeResult.declined("Your card was declined"))
                .when(paymentGateway).chargeSavedMethod(any(), anyString());
        returnRental();

        waive(new BigDecimal("500.00"));

        Payment remainder = penaltyPayment();
        assertThat(remainder.getStatus()).isEqualTo(PaymentStatus.FAILED);
        awaitPenaltyAlert(remainder.getId(), "declined");
    }

    @Test
    @DisplayName("a waiver resolves open payments and charges the remainder without a transaction open around gateway calls")
    void waiverDoesNotHoldATransactionAroundGatewayCalls() throws Exception {
        givenOverdueRental(2);
        java.util.Map<String, java.util.List<Boolean>> inTransaction = new java.util.concurrent.ConcurrentHashMap<>();
        java.util.concurrent.atomic.AtomicInteger charges = new java.util.concurrent.atomic.AtomicInteger();
        doAnswer(invocation -> {
            if (charges.getAndIncrement() == 0) {
                return ChargeResult.ambiguous("Gateway timeout");
            }
            inTransaction.computeIfAbsent("charge", k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                    .add(TransactionSynchronizationManager.isActualTransactionActive());
            return invocation.callRealMethod();
        }).when(paymentGateway).chargeSavedMethod(any(), anyString());
        returnRental();
        Payment pending = penaltyPayment();
        doAnswer(invocation -> {
            inTransaction.computeIfAbsent("find", k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                    .add(TransactionSynchronizationManager.isActualTransactionActive());
            return Optional.of(new GatewayIntent("pi_open", null, GatewayIntentStatus.REQUIRES_PAYMENT_METHOD, "pm_test",
                    "cus_test", 150000L, "try"));
        }).when(paymentGateway).findIntentByPayment(pending.getId());
        doAnswer(invocation -> {
            inTransaction.computeIfAbsent("cancel", k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                    .add(TransactionSynchronizationManager.isActualTransactionActive());
            return invocation.callRealMethod();
        }).when(paymentGateway).cancel(anyString(), anyString());

        waive(new BigDecimal("500.00"));

        assertThat(inTransaction).containsKeys("find", "cancel", "charge");
        inTransaction.forEach((call, flags) -> assertThat(flags).as(call).isNotEmpty().containsOnly(false));
        assertThat(penaltyPayment().getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(reload().getPenaltyPaid()).isTrue();
    }
}
