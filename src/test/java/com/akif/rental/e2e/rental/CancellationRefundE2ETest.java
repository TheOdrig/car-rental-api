package com.akif.rental.e2e.rental;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.payment.domain.Payment;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.rental.api.RentalCancelledEvent;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.api.RentalRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.akif.testsupport.TestcontainersConfiguration;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@DisplayName("Cancellation and Refund E2E Tests")
@Slf4j
class CancellationRefundE2ETest extends E2ETestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Nested
    @DisplayName("REQUESTED Rental Cancellation Tests")
    class RequestedRentalCancellationTests {

        @Test
        @DisplayName("Should cancel REQUESTED rental without payment operations")
        void shouldCancelRequestedRentalWithoutPayment() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            Optional<Payment> paymentOptional = paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rentalId, com.akif.payment.domain.enums.PaymentKind.RENTAL);
            assertThat(paymentOptional).isEmpty();

            mockMvc.perform(post("/api/v1/rentals/{id}/cancel", rentalId)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andExpect(jsonPath("$.status").value(RentalStatus.CANCELLED.getDisplayName()));

            paymentOptional = paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rentalId, com.akif.payment.domain.enums.PaymentKind.RENTAL);
            assertThat(paymentOptional).isEmpty();

            log.info("Successfully cancelled REQUESTED rental without payment operations");
        }
    }

    @Nested
    @DisplayName("CONFIRMED Rental Cancellation Tests")
    class ConfirmedRentalCancellationTests {

        @Test
        @DisplayName("Should cancel CONFIRMED rental and release the authorization hold")
        void shouldCancelConfirmedRentalWithRefund() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            User testAdmin = TestDataBuilder.createTestAdmin();
            testAdmin = userRepository.save(testAdmin);
            String adminToken = generateAdminToken(testAdmin);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            mockMvc.perform(post("/api/v1/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(RentalStatus.CONFIRMED.getDisplayName()));

            Payment payment = paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rentalId, com.akif.payment.domain.enums.PaymentKind.RENTAL)
                    .orElseThrow(() -> new AssertionError("Payment not found for rental: " + rentalId));
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);

            payRental(rentalId);
            payment = paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rentalId, com.akif.payment.domain.enums.PaymentKind.RENTAL)
                    .orElseThrow(() -> new AssertionError("Payment not found for rental: " + rentalId));
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);

            eventCaptor.clear();

            mockMvc.perform(post("/api/v1/rentals/{id}/cancel", rentalId)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andExpect(jsonPath("$.status").value(RentalStatus.CANCELLED.getDisplayName()));

            payment = paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rentalId, com.akif.payment.domain.enums.PaymentKind.RENTAL)
                    .orElseThrow(() -> new AssertionError("Payment not found for rental: " + rentalId));
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);

            List<RentalCancelledEvent> cancelledEvents = eventCaptor.getEventsOfType(RentalCancelledEvent.class);
            assertThat(cancelledEvents).hasSize(1);

            RentalCancelledEvent event = cancelledEvents.get(0);
            assertThat(event.rentalId()).isEqualTo(rentalId);
            assertThat(event.customerEmail()).isEqualTo(testUser.getEmail());
            assertThat(event.refundProcessed()).isTrue();
            assertThat(event.refundAmount()).isNotNull();
            assertThat(event.refundTransactionId()).isNotNull();

            log.info("Successfully cancelled CONFIRMED rental with refund");
        }

        @Test
        @DisplayName("Should cancel an unpaid CONFIRMED rental and cancel the pending payment without a refund")
        void shouldCancelUnpaidConfirmedRental() throws Exception {
            User testUser = userRepository.save(TestDataBuilder.createTestUser());
            String userToken = generateUserToken(testUser);
            User testAdmin = userRepository.save(TestDataBuilder.createTestAdmin());
            String adminToken = generateAdminToken(testAdmin);
            Car testCar = carRepository.save(TestDataBuilder.createAvailableCar());

            Long rentalId = createAndGetRentalId(TestDataBuilder.createRentalRequest(testCar.getId()), userToken);
            mockMvc.perform(post("/api/v1/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
            eventCaptor.clear();

            mockMvc.perform(post("/api/v1/rentals/{id}/cancel", rentalId)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk());

            Payment payment = paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rentalId, com.akif.payment.domain.enums.PaymentKind.RENTAL)
                    .orElseThrow(() -> new AssertionError("Payment not found for rental: " + rentalId));
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);

            RentalCancelledEvent event = eventCaptor.getEventsOfType(RentalCancelledEvent.class).get(0);
            assertThat(event.refundProcessed()).isFalse();
        }
    }

    @Nested
    @DisplayName("IN_USE Rental Cancellation Tests")
    class InUseRentalCancellationTests {

        @Test
        @DisplayName("Should cancel IN_USE rental and process partial refund")
        void shouldCancelInUseRentalWithPartialRefund() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            User testAdmin = TestDataBuilder.createTestAdmin();
            testAdmin = userRepository.save(testAdmin);
            String adminToken = generateAdminToken(testAdmin);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            mockMvc.perform(post("/api/v1/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(RentalStatus.CONFIRMED.getDisplayName()));

            payRental(rentalId);
            mockMvc.perform(post("/api/v1/rentals/{id}/pickup", rentalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(RentalStatus.IN_USE.getDisplayName()));

            Payment payment = paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rentalId, com.akif.payment.domain.enums.PaymentKind.RENTAL)
                    .orElseThrow(() -> new AssertionError("Payment not found for rental: " + rentalId));
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
            BigDecimal originalAmount = payment.getAmount();

            eventCaptor.clear();

            mockMvc.perform(post("/api/v1/rentals/{id}/cancel", rentalId)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andExpect(jsonPath("$.status").value(RentalStatus.CANCELLED.getDisplayName()));

            payment = paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rentalId, com.akif.payment.domain.enums.PaymentKind.RENTAL)
                    .orElseThrow(() -> new AssertionError("Payment not found for rental: " + rentalId));
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

            List<RentalCancelledEvent> cancelledEvents = eventCaptor.getEventsOfType(RentalCancelledEvent.class);
            assertThat(cancelledEvents).hasSize(1);

            RentalCancelledEvent event = cancelledEvents.get(0);
            assertThat(event.rentalId()).isEqualTo(rentalId);
            assertThat(event.customerEmail()).isEqualTo(testUser.getEmail());
            assertThat(event.refundProcessed()).isTrue();
            assertThat(event.refundAmount()).isNotNull();

            assertThat(event.refundAmount()).isLessThanOrEqualTo(originalAmount);
            assertThat(event.refundTransactionId()).isNotNull();

            log.info("Successfully cancelled IN_USE rental with partial refund");
        }
    }
}
