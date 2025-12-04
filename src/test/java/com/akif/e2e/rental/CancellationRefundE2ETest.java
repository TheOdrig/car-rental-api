package com.akif.e2e.rental;

import com.akif.dto.request.RentalRequestDto;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.enums.PaymentStatus;
import com.akif.enums.RentalStatus;
import com.akif.event.RentalCancelledEvent;
import com.akif.model.Car;
import com.akif.model.Payment;
import com.akif.model.User;
import com.akif.repository.CarRepository;
import com.akif.repository.PaymentRepository;
import com.akif.repository.UserRepository;
import com.akif.starter.CarGalleryProjectApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
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

            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            Optional<Payment> paymentOptional = paymentRepository.findByRentalIdAndIsDeletedFalse(rentalId);
            assertThat(paymentOptional).isEmpty();

            mockMvc.perform(post("/api/rentals/{id}/cancel", rentalId)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andExpect(jsonPath("$.status").value(RentalStatus.CANCELLED.getDisplayName()));

            paymentOptional = paymentRepository.findByRentalIdAndIsDeletedFalse(rentalId);
            assertThat(paymentOptional).isEmpty();

            log.info("Successfully cancelled REQUESTED rental without payment operations");
        }
    }

    @Nested
    @DisplayName("CONFIRMED Rental Cancellation Tests")
    class ConfirmedRentalCancellationTests {

        @Test
        @DisplayName("Should cancel CONFIRMED rental and process refund")
        void shouldCancelConfirmedRentalWithRefund() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            User testAdmin = TestDataBuilder.createTestAdmin();
            testAdmin = userRepository.save(testAdmin);
            String adminToken = generateAdminToken(testAdmin);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(RentalStatus.CONFIRMED.getDisplayName()));

            Payment payment = paymentRepository.findByRentalIdAndIsDeletedFalse(rentalId)
                    .orElseThrow(() -> new AssertionError("Payment not found for rental: " + rentalId));
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);

            eventCaptor.clear();

            mockMvc.perform(post("/api/rentals/{id}/cancel", rentalId)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andExpect(jsonPath("$.status").value(RentalStatus.CANCELLED.getDisplayName()));

            payment = paymentRepository.findByRentalIdAndIsDeletedFalse(rentalId)
                    .orElseThrow(() -> new AssertionError("Payment not found for rental: " + rentalId));
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

            List<RentalCancelledEvent> cancelledEvents = eventCaptor.getEventsOfType(RentalCancelledEvent.class);
            assertThat(cancelledEvents).hasSize(1);

            RentalCancelledEvent event = cancelledEvents.get(0);
            assertThat(event.getRentalId()).isEqualTo(rentalId);
            assertThat(event.getCustomerEmail()).isEqualTo(testUser.getEmail());
            assertThat(event.isRefundProcessed()).isTrue();
            assertThat(event.getRefundAmount()).isNotNull();
            assertThat(event.getRefundTransactionId()).isNotNull();

            log.info("Successfully cancelled CONFIRMED rental with refund");
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

            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(RentalStatus.CONFIRMED.getDisplayName()));

            mockMvc.perform(post("/api/rentals/{id}/pickup", rentalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(RentalStatus.IN_USE.getDisplayName()));

            Payment payment = paymentRepository.findByRentalIdAndIsDeletedFalse(rentalId)
                    .orElseThrow(() -> new AssertionError("Payment not found for rental: " + rentalId));
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
            BigDecimal originalAmount = payment.getAmount();

            eventCaptor.clear();

            mockMvc.perform(post("/api/rentals/{id}/cancel", rentalId)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andExpect(jsonPath("$.status").value(RentalStatus.CANCELLED.getDisplayName()));

            payment = paymentRepository.findByRentalIdAndIsDeletedFalse(rentalId)
                    .orElseThrow(() -> new AssertionError("Payment not found for rental: " + rentalId));
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

            List<RentalCancelledEvent> cancelledEvents = eventCaptor.getEventsOfType(RentalCancelledEvent.class);
            assertThat(cancelledEvents).hasSize(1);

            RentalCancelledEvent event = cancelledEvents.get(0);
            assertThat(event.getRentalId()).isEqualTo(rentalId);
            assertThat(event.getCustomerEmail()).isEqualTo(testUser.getEmail());
            assertThat(event.isRefundProcessed()).isTrue();
            assertThat(event.getRefundAmount()).isNotNull();

            assertThat(event.getRefundAmount()).isLessThanOrEqualTo(originalAmount);
            assertThat(event.getRefundTransactionId()).isNotNull();

            log.info("Successfully cancelled IN_USE rental with partial refund");
        }
    }
}
