package com.akif.payment.e2e;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.payment.api.CreatePaymentRequest;
import com.akif.payment.api.PaymentCancelledEvent;
import com.akif.payment.api.PaymentCapturedEvent;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.Payment;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import com.akif.payment.internal.service.reconciliation.PaymentRepairService;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@DisplayName("Payment repair E2E Tests")
class PaymentRepairE2ETest extends E2ETestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentGateway paymentGateway;

    @Autowired
    private PaymentRepairService repairService;

    private static final java.util.concurrent.atomic.AtomicLong NEXT_PAYMENT_ID = new java.util.concurrent.atomic.AtomicLong(7000);

    private Payment authorizedPayment() {
        jdbcTemplate.queryForObject("SELECT setval(pg_get_serial_sequence('car_rental.payments', 'id'), ?)",
                Long.class, NEXT_PAYMENT_ID.addAndGet(10));
        User user = userRepository.save(TestDataBuilder.createTestUser());
        Car car = carRepository.save(TestDataBuilder.createAvailableCar());
        LocalDate today = LocalDate.now(clock);
        Rental rental = rentalRepository.save(TestDataBuilder.createRental(
                car, user, today.plusDays(1), today.plusDays(4), RentalStatus.CONFIRMED));
        paymentService.createPayment(new CreatePaymentRequest(
                rental.getId(), rental.getUserEmail(), rental.getCarLicensePlate(),
                rental.getTotalPrice(), rental.getCurrency(), "CARD"));
        paymentService.createOrGetIntentForRental(rental.getId());
        Payment payment = paymentRepository.findAll().get(0);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        return payment;
    }

    private void makeStale(Payment payment) {
        jdbcTemplate.update("UPDATE car_rental.payments SET update_time = ?, last_synced_at = NULL WHERE id = ?",
                java.sql.Timestamp.from(Instant.now(clock).minus(Duration.ofHours(2))), payment.getId());
    }

    private Payment reload(Payment payment) {
        return paymentRepository.findById(payment.getId()).orElseThrow();
    }

    @Test
    @DisplayName("a capture whose webhook was lost is applied from the gateway")
    void lostCaptureWebhookIsRepaired() {
        Payment payment = authorizedPayment();
        paymentGateway.capture(payment.getStripePaymentIntentId(), "key-capture");
        makeStale(payment);
        eventCaptor.clear();

        int repaired = repairService.repairStalePayments();

        assertThat(repaired).isEqualTo(1);
        Payment repairedPayment = reload(payment);
        assertThat(repairedPayment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(repairedPayment.getCapturedAt()).isNotNull();
        assertThat(eventCaptor.getEventsOfType(PaymentCapturedEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("an authorization that expired at the gateway is closed locally and announced")
    void lostCancellationWebhookIsRepaired() {
        Payment payment = authorizedPayment();
        paymentGateway.cancel(payment.getStripePaymentIntentId(), "key-cancel");
        makeStale(payment);
        eventCaptor.clear();

        int repaired = repairService.repairStalePayments();

        assertThat(repaired).isEqualTo(1);
        assertThat(reload(payment).getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        await().atMost(Duration.ofSeconds(5)).untilAsserted(
                () -> assertThat(eventCaptor.getEventsOfType(PaymentCancelledEvent.class)).hasSize(1));
    }

    @Test
    @DisplayName("a payment that is still in step with the gateway is left alone and is not read again soon")
    void unchangedPaymentIsNotRepairedTwice() {
        Payment payment = authorizedPayment();
        makeStale(payment);

        assertThat(repairService.repairStalePayments()).isZero();

        assertThat(reload(payment).getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(reload(payment).getLastSyncedAt()).isNotNull();
        paymentGateway.capture(payment.getStripePaymentIntentId(), "key-capture");
        assertThat(repairService.repairStalePayments()).isZero();
        assertThat(reload(payment).getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    }

    @Test
    @DisplayName("a payment that changed recently is not touched")
    void recentPaymentsAreSkipped() {
        Payment payment = authorizedPayment();
        paymentGateway.capture(payment.getStripePaymentIntentId(), "key-capture");

        assertThat(repairService.repairStalePayments()).isZero();

        assertThat(reload(payment).getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    }
}
