package com.akif.payment.e2e;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.payment.api.CreatePaymentRequest;
import com.akif.payment.api.PaymentCapturedEvent;
import com.akif.payment.api.PaymentDto;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.Payment;
import com.akif.payment.domain.enums.PaymentKind;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.shared.enums.Role;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@DisplayName("Payment flow E2E Tests")
class PaymentFlowE2ETest extends E2ETestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @MockitoSpyBean
    private PaymentGateway paymentGateway;

    private User customer;
    private User otherCustomer;
    private User admin;
    private String customerToken;
    private String otherToken;
    private String adminToken;
    private Car car;

    private Long confirmedRentalId() throws Exception {
        customer = userRepository.save(TestDataBuilder.createTestUser());
        otherCustomer = userRepository.save(TestDataBuilder.createTestUser("otherrenter", Role.USER));
        admin = userRepository.save(TestDataBuilder.createTestAdmin());
        customerToken = generateUserToken(customer);
        otherToken = generateUserToken(otherCustomer);
        adminToken = generateAdminToken(admin);
        car = carRepository.save(TestDataBuilder.createAvailableCar());

        Long rentalId = createAndGetRentalId(TestDataBuilder.createRentalRequest(car.getId()), customerToken);
        mockMvc.perform(post("/api/v1/rentals/{id}/confirm", rentalId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        return rentalId;
    }

    private Payment rentalPayment(Long rentalId) {
        return paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rentalId, PaymentKind.RENTAL).orElseThrow();
    }

    private void pickup(Long rentalId) throws Exception {
        mockMvc.perform(post("/api/v1/rentals/{id}/pickup", rentalId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Nested
    @DisplayName("Confirmation")
    class Confirmation {

        @Test
        @DisplayName("confirm creates a PENDING rental payment with the server-computed total and no gateway call")
        void confirmCreatesPendingPayment() throws Exception {
            Long rentalId = confirmedRentalId();

            Payment payment = rentalPayment(rentalId);
            Rental rental = rentalRepository.findById(rentalId).orElseThrow();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.getAmount()).isEqualByComparingTo(rental.getTotalPrice());
            assertThat(payment.getCurrency()).isEqualTo(rental.getCurrency());
            assertThat(payment.getStripePaymentIntentId()).isNull();
            verify(paymentGateway, times(0)).createIntent(any(), anyString());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/rentals/{id}/payment-intent")
    class PaymentIntentEndpoint {

        @Test
        @DisplayName("returns the client secret and the server-computed amount to the rental owner")
        void ownerGetsIntent() throws Exception {
            Long rentalId = confirmedRentalId();
            Rental rental = rentalRepository.findById(rentalId).orElseThrow();

            mockMvc.perform(post("/api/v1/rentals/{id}/payment-intent", rentalId)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rentalId").value(rentalId))
                    .andExpect(jsonPath("$.paymentId").value(rentalPayment(rentalId).getId()))
                    .andExpect(jsonPath("$.clientSecret").value(org.hamcrest.Matchers.startsWith("stub_secret_")))
                    .andExpect(jsonPath("$.paymentIntentId").value(org.hamcrest.Matchers.startsWith("stub_pi_")))
                    .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                    .andExpect(jsonPath("$.amount").value(rental.getTotalPrice().doubleValue()))
                    .andExpect(jsonPath("$.currency").value(rental.getCurrency().name()));

            assertThat(rentalPayment(rentalId).getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        }

        @Test
        @DisplayName("ignores any amount sent by the client")
        void clientAmountIsIgnored() throws Exception {
            Long rentalId = confirmedRentalId();
            Rental rental = rentalRepository.findById(rentalId).orElseThrow();

            mockMvc.perform(post("/api/v1/rentals/{id}/payment-intent", rentalId)
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":0.01,\"currency\":\"JPY\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.amount").value(rental.getTotalPrice().doubleValue()))
                    .andExpect(jsonPath("$.currency").value(rental.getCurrency().name()));
        }

        @Test
        @DisplayName("is idempotent: a second call returns the same intent")
        void idempotent() throws Exception {
            Long rentalId = confirmedRentalId();

            String first = mockMvc.perform(post("/api/v1/rentals/{id}/payment-intent", rentalId)
                            .header("Authorization", "Bearer " + customerToken))
                    .andReturn().getResponse().getContentAsString();
            String second = mockMvc.perform(post("/api/v1/rentals/{id}/payment-intent", rentalId)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(objectMapper.readTree(second).get("paymentIntentId").asString())
                    .isEqualTo(objectMapper.readTree(first).get("paymentIntentId").asString());
            verify(paymentGateway, times(1)).createIntent(any(), anyString());
        }

        @Test
        @DisplayName("is forbidden for a customer who does not own the rental")
        void otherCustomerForbidden() throws Exception {
            Long rentalId = confirmedRentalId();

            mockMvc.perform(post("/api/v1/rentals/{id}/payment-intent", rentalId)
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isForbidden());

            verify(paymentGateway, times(0)).createIntent(any(), anyString());
        }

        @Test
        @DisplayName("is forbidden for an admin-only token")
        void adminOnlyForbidden() throws Exception {
            Long rentalId = confirmedRentalId();

            mockMvc.perform(post("/api/v1/rentals/{id}/payment-intent", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("requires authentication")
        void unauthenticated() throws Exception {
            Long rentalId = confirmedRentalId();

            mockMvc.perform(post("/api/v1/rentals/{id}/payment-intent", rentalId))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("is rejected while the rental is still REQUESTED")
        void requestedRentalRejected() throws Exception {
            customer = userRepository.save(TestDataBuilder.createTestUser());
            customerToken = generateUserToken(customer);
            car = carRepository.save(TestDataBuilder.createAvailableCar());
            Long rentalId = createAndGetRentalId(TestDataBuilder.createRentalRequest(car.getId()), customerToken);

            mockMvc.perform(post("/api/v1/rentals/{id}/payment-intent", rentalId)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("answers 404 for an unknown rental")
        void unknownRental() throws Exception {
            confirmedRentalId();

            mockMvc.perform(post("/api/v1/rentals/{id}/payment-intent", 987654L)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("is rejected once the rental is already in use")
        void inUseRentalRejected() throws Exception {
            Long rentalId = confirmedRentalId();
            payRental(rentalId);
            pickup(rentalId);

            mockMvc.perform(post("/api/v1/rentals/{id}/payment-intent", rentalId)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/rentals/{id}/payment")
    class PaymentStatusEndpoint {

        @Test
        @DisplayName("shows the status to the owner and to an admin but not to another customer")
        void visibility() throws Exception {
            Long rentalId = confirmedRentalId();
            payRental(rentalId);

            mockMvc.perform(get("/api/v1/rentals/{id}/payment", rentalId)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                    .andExpect(jsonPath("$.rentalId").value(rentalId));
            mockMvc.perform(get("/api/v1/rentals/{id}/payment", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/v1/rentals/{id}/payment", rentalId)
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("answers 404 while the rental has no payment yet")
        void noPaymentYet() throws Exception {
            customer = userRepository.save(TestDataBuilder.createTestUser());
            customerToken = generateUserToken(customer);
            car = carRepository.save(TestDataBuilder.createAvailableCar());
            Long rentalId = createAndGetRentalId(TestDataBuilder.createRentalRequest(car.getId()), customerToken);

            mockMvc.perform(get("/api/v1/rentals/{id}/payment", rentalId)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Capture at pickup")
    class CaptureAtPickup {

        @Test
        @DisplayName("captures the authorized payment and publishes a single PaymentCapturedEvent")
        void captureOnPickup() throws Exception {
            Long rentalId = confirmedRentalId();
            payRental(rentalId);
            eventCaptor.clear();

            pickup(rentalId);

            assertThat(rentalPayment(rentalId).getStatus()).isEqualTo(PaymentStatus.CAPTURED);
            assertThat(eventCaptor.getEventsOfType(PaymentCapturedEvent.class)).hasSize(1);
        }

        @Test
        @DisplayName("refuses pickup while the customer has not paid")
        void pickupRequiresPayment() throws Exception {
            Long rentalId = confirmedRentalId();

            mockMvc.perform(post("/api/v1/rentals/{id}/pickup", rentalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isConflict());

            assertThat(rentalPayment(rentalId).getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("proceeds without a second capture when a webhook already captured the payment")
        void pickupAfterWebhookCapture() throws Exception {
            Long rentalId = confirmedRentalId();
            payRental(rentalId);
            Payment payment = rentalPayment(rentalId);
            payment.capture();
            paymentRepository.save(payment);
            eventCaptor.clear();

            pickup(rentalId);

            verify(paymentGateway, times(0)).capture(anyString(), anyString());
            assertThat(eventCaptor.getEventsOfType(PaymentCapturedEvent.class)).isEmpty();
        }

        @Test
        @DisplayName("uses the same idempotency key when a failed capture is retried")
        void captureRetryKeepsKey() throws Exception {
            Long rentalId = confirmedRentalId();
            payRental(rentalId);
            doReturn(PaymentResult.failure("network"), PaymentResult.success("x", "ok"))
                    .when(paymentGateway).capture(anyString(), anyString());

            mockMvc.perform(post("/api/v1/rentals/{id}/pickup", rentalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isPaymentRequired());
            pickup(rentalId);

            ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
            verify(paymentGateway, times(2)).capture(anyString(), keys.capture());
            assertThat(keys.getAllValues().get(0)).isEqualTo(keys.getAllValues().get(1));
        }
    }

    @Nested
    @DisplayName("Refunds")
    class Refunds {

        @Autowired
        private com.akif.payment.api.PaymentService paymentService;

        private Long capturedPaymentId() throws Exception {
            Long rentalId = confirmedRentalId();
            payRental(rentalId);
            pickup(rentalId);
            return rentalPayment(rentalId).getId();
        }

        @Test
        @DisplayName("refuses to refund more than the remaining balance")
        void overRefundRejected() throws Exception {
            Long paymentId = capturedPaymentId();
            BigDecimal amount = paymentRepository.findById(paymentId).orElseThrow().getAmount();

            PaymentResult result = paymentService.refundPayment(paymentId, amount.add(BigDecimal.ONE), "op-over");

            assertThat(result.success()).isFalse();
            verify(paymentGateway, times(0)).refund(anyString(), any(), any(), anyString());
        }

        @Test
        @DisplayName("refuses a zero or negative refund")
        void nonPositiveRefundRejected() throws Exception {
            Long paymentId = capturedPaymentId();

            assertThat(paymentService.refundPayment(paymentId, BigDecimal.ZERO, "op-zero").success()).isFalse();
            assertThat(paymentService.refundPayment(paymentId, new BigDecimal("-5"), "op-neg").success()).isFalse();
        }

        @Test
        @DisplayName("refuses a refund on a payment that was never captured")
        void refundRequiresCapture() throws Exception {
            Long rentalId = confirmedRentalId();
            payRental(rentalId);

            PaymentResult result = paymentService.refundPayment(rentalPayment(rentalId).getId(), BigDecimal.ONE, "op-uncaptured");

            assertThat(result.success()).isFalse();
        }

        @Test
        @DisplayName("keeps the idempotency key when a failed refund is retried and uses a new key for another operation")
        void refundKeySequence() throws Exception {
            Long paymentId = capturedPaymentId();
            BigDecimal amount = paymentRepository.findById(paymentId).orElseThrow().getAmount();
            BigDecimal part = amount.divide(new BigDecimal("4"), 2, java.math.RoundingMode.DOWN);
            doReturn(PaymentResult.failure("network"))
                    .doCallRealMethod()
                    .doCallRealMethod()
                    .when(paymentGateway).refund(anyString(), any(), any(), anyString());

            assertThat(paymentService.refundPayment(paymentId, part, "op-a").success()).isFalse();
            assertThat(paymentService.refundPayment(paymentId, part, "op-a").success()).isTrue();
            assertThat(paymentService.refundPayment(paymentId, part, "op-b").success()).isTrue();

            ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
            verify(paymentGateway, times(3)).refund(anyString(), any(), any(), keys.capture());
            assertThat(keys.getAllValues().get(0)).isEqualTo(keys.getAllValues().get(1));
            assertThat(keys.getAllValues().get(2)).isNotEqualTo(keys.getAllValues().get(1));
            assertThat(paymentRepository.findById(paymentId).orElseThrow().getRefundedAmount())
                    .isEqualByComparingTo(part.multiply(new BigDecimal("2")));
        }

        @Test
        @DisplayName("marks the payment REFUNDED once the full amount has been refunded")
        void fullRefundFlipsStatus() throws Exception {
            Long paymentId = capturedPaymentId();
            BigDecimal amount = paymentRepository.findById(paymentId).orElseThrow().getAmount();

            assertThat(paymentService.refundPayment(paymentId, amount, "op-full").success()).isTrue();

            assertThat(paymentRepository.findById(paymentId).orElseThrow().getStatus())
                    .isEqualTo(PaymentStatus.REFUNDED);
            assertThat(paymentService.refundPayment(paymentId, BigDecimal.ONE, "op-after-full").success()).isFalse();
        }

        @Test
        @DisplayName("refund by transaction id resolves the payment and applies the same limits")
        void refundByTransactionId() throws Exception {
            Long paymentId = capturedPaymentId();
            Payment payment = paymentRepository.findById(paymentId).orElseThrow();

            assertThat(paymentService.refund(payment.getTransactionId(), payment.getAmount().add(BigDecimal.ONE), "op-tx-over").success())
                    .isFalse();
            assertThat(paymentService.refund(payment.getTransactionId(), BigDecimal.ONE, "op-tx").success()).isTrue();
            assertThat(paymentService.refund("unknown-transaction", BigDecimal.ONE, "op-unknown").success()).isFalse();
        }
    }

    @Nested
    @DisplayName("Saved-method charges")
    class SavedMethodCharges {

        @Autowired
        private com.akif.payment.api.PaymentService paymentService;

        @Test
        @DisplayName("charges a penalty against the saved method and marks it CAPTURED without a receipt event")
        void penaltyCharge() throws Exception {
            Long rentalId = confirmedRentalId();
            payRental(rentalId);
            pickup(rentalId);
            Rental rental = rentalRepository.findById(rentalId).orElseThrow();
            PaymentDto penalty = paymentService.createPayment(new CreatePaymentRequest(
                    rentalId, rental.getUserEmail(), rental.getCarLicensePlate(),
                    new BigDecimal("25.00"), rental.getCurrency(), "PENALTY"));
            eventCaptor.clear();

            PaymentResult result = paymentService.chargePayment(penalty.id());

            assertThat(result.success()).isTrue();
            assertThat(paymentRepository.findById(penalty.id()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CAPTURED);
            assertThat(eventCaptor.getEventsOfType(PaymentCapturedEvent.class)).isEmpty();
            assertThat(paymentService.getPaymentByRentalId(rentalId)).isPresent();
        }

        @Test
        @DisplayName("fails a penalty charge when the rental has no saved payment method")
        void penaltyWithoutSavedMethod() throws Exception {
            Long rentalId = confirmedRentalId();
            Rental rental = rentalRepository.findById(rentalId).orElseThrow();
            PaymentDto penalty = paymentService.createPayment(new CreatePaymentRequest(
                    rentalId, rental.getUserEmail(), rental.getCarLicensePlate(),
                    new BigDecimal("25.00"), rental.getCurrency(), "PENALTY"));

            PaymentResult result = paymentService.chargePayment(penalty.id());

            assertThat(result.success()).isFalse();
            assertThat(paymentRepository.findById(penalty.id()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("charges damage in the rental currency and is idempotent per damage report")
        void damageCharge() throws Exception {
            Long rentalId = confirmedRentalId();
            payRental(rentalId);
            pickup(rentalId);
            Rental rental = rentalRepository.findById(rentalId).orElseThrow();

            PaymentResult first = paymentService.chargeDamage(rentalId, 77L, new BigDecimal("300.00"), null);
            PaymentResult second = paymentService.chargeDamage(rentalId, 77L, new BigDecimal("300.00"), null);
            PaymentResult changed = paymentService.chargeDamage(rentalId, 77L, new BigDecimal("350.00"), null);

            assertThat(first.success()).isTrue();
            assertThat(second.success()).isTrue();
            assertThat(second.transactionId()).isEqualTo(first.transactionId());
            assertThat(changed.success()).isTrue();
            List<Payment> damagePayments = paymentRepository.findAll().stream()
                    .filter(p -> p.getKind() == PaymentKind.DAMAGE && p.getRentalId().equals(rentalId))
                    .toList();
            assertThat(damagePayments).hasSize(1);
            assertThat(damagePayments.get(0).getCurrency()).isEqualTo(rental.getCurrency());
            assertThat(damagePayments.get(0).getAmount()).isEqualByComparingTo("300.00");
        }

        @Test
        @DisplayName("fails a damage charge when the rental has no payment")
        void damageWithoutPayment() throws Exception {
            confirmedRentalId();

            PaymentResult result = paymentService.chargeDamage(123456L, 5L, new BigDecimal("10.00"), null);

            assertThat(result.success()).isFalse();
        }
    }

    @Test
    @DisplayName("cancelling a paid but uncaptured rental releases the hold exactly once")
    void cancelReleasesHoldOnce() throws Exception {
        Long rentalId = confirmedRentalId();
        payRental(rentalId);

        mockMvc.perform(post("/api/v1/rentals/{id}/cancel", rentalId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        assertThat(rentalPayment(rentalId).getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(paymentGateway, times(1)).cancel(anyString(), anyString());
    }

    @Nested
    @DisplayName("Payment window and pickup errors")
    class WindowAndPickupErrors {

        @Autowired
        private com.akif.rental.internal.config.RentalProperties rentalProperties;

        @Test
        @DisplayName("payment-intent before the window opens answers 409 PAYMENT_WINDOW_NOT_OPEN with the opening date")
        void windowNotOpen() throws Exception {
            Long rentalId = confirmedRentalId();
            Rental rental = rentalRepository.findById(rentalId).orElseThrow();
            int original = rentalProperties.getPaymentWindowDays();
            rentalProperties.setPaymentWindowDays(0);
            try {
                java.time.LocalDate opensOn = rental.getStartDate();
                mockMvc.perform(post("/api/v1/rentals/{id}/payment-intent", rentalId)
                                .header("Authorization", "Bearer " + customerToken))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.code").value("PAYMENT_WINDOW_NOT_OPEN"))
                        .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(opensOn.toString())))
                        .andExpect(jsonPath("$.details.paymentOpensOn").value(opensOn.toString()));
                verify(paymentGateway, times(0)).createIntent(any(), anyString());
            } finally {
                rentalProperties.setPaymentWindowDays(original);
            }
        }

        @Test
        @DisplayName("payment-intent inside the window is allowed")
        void windowOpen() throws Exception {
            Long rentalId = confirmedRentalId();
            int original = rentalProperties.getPaymentWindowDays();
            rentalProperties.setPaymentWindowDays(5);
            try {
                mockMvc.perform(post("/api/v1/rentals/{id}/payment-intent", rentalId)
                                .header("Authorization", "Bearer " + customerToken))
                        .andExpect(status().isOk());
            } finally {
                rentalProperties.setPaymentWindowDays(original);
            }
        }

        @Test
        @DisplayName("pickup without an authorized payment answers 409 PAYMENT_NOT_AUTHORIZED")
        void pickupWithoutAuthorizedPayment() throws Exception {
            Long rentalId = confirmedRentalId();

            mockMvc.perform(post("/api/v1/rentals/{id}/pickup", rentalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PAYMENT_NOT_AUTHORIZED"));
        }

        @Test
        @DisplayName("pickup with no payment on record answers 404 PAYMENT_NOT_FOUND")
        void pickupWithoutPayment() throws Exception {
            Long rentalId = confirmedRentalId();
            paymentRepository.delete(rentalPayment(rentalId));
            paymentRepository.flush();

            mockMvc.perform(post("/api/v1/rentals/{id}/pickup", rentalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
        }

        @Test
        @DisplayName("repeated GET /payment inside the throttle interval calls Stripe once")
        void statusRefreshIsThrottled() throws Exception {
            Long rentalId = confirmedRentalId();
            Payment payment = rentalPayment(rentalId);
            payment.attachIntent("stub_pi_throttle", "k");
            paymentRepository.saveAndFlush(payment);
            doReturn(new com.akif.payment.internal.service.gateway.GatewayIntent(
                    "stub_pi_throttle", null,
                    com.akif.payment.internal.service.gateway.GatewayIntentStatus.REQUIRES_PAYMENT_METHOD,
                    null, null, 0L, "usd")).when(paymentGateway).retrieveIntent("stub_pi_throttle");

            for (int i = 0; i < 4; i++) {
                mockMvc.perform(get("/api/v1/rentals/{id}/payment", rentalId)
                                .header("Authorization", "Bearer " + customerToken))
                        .andExpect(status().isOk());
            }

            verify(paymentGateway, times(1)).retrieveIntent("stub_pi_throttle");
        }
    }
}
