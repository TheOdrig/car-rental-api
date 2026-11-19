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
import com.akif.payment.internal.service.PaymentStateService;
import com.akif.payment.internal.service.gateway.ChargeResult;
import com.akif.payment.internal.service.gateway.PaymentGateway;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
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
    private PlatformTransactionManager transactionManager;

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
    @DisplayName("waiving an unpaid penalty closes the outstanding payment so a late confirmation cannot collect it")
    void waivingAnUnpaidPenaltyClosesThePayment() throws Exception {
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

        assertThat(paymentRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(reload().getPenaltyPaid()).isFalse();
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
}
