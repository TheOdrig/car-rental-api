package com.akif.rental.e2e.rental;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.e2e.infrastructure.TestFixtures;
import com.akif.payment.api.PaymentCapturedEvent;
import com.akif.payment.domain.Payment;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.rental.api.RentalConfirmedEvent;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.api.RentalRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.akif.testsupport.TestcontainersConfiguration;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@DisplayName("Rental Lifecycle E2E Tests")
class RentalLifecycleE2ETest extends E2ETestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Nested
    @DisplayName("Complete Rental Lifecycle Tests")
    class CompleteRentalLifecycleTests {

        @Test
        @DisplayName("Should complete full rental lifecycle: request → confirm → pickup → return")
        void shouldCompleteFullRentalLifecycle() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            User testAdmin = TestDataBuilder.createTestAdmin();
            testAdmin = userRepository.save(testAdmin);
            String adminToken = generateAdminToken(testAdmin);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());

            String responseJson = mockMvc.perform(post("/api/v1/rentals/request")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rentalRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.status").value(RentalStatus.REQUESTED.name()))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RentalResponse rentalResponse = objectMapper.readValue(responseJson, RentalResponse.class);
            Long rentalId = rentalResponse.id();
            assertThat(rentalId).isNotNull();

            mockMvc.perform(post("/api/v1/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andExpect(jsonPath("$.status").value(RentalStatus.CONFIRMED.name()));

            Payment payment = paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rentalId, com.akif.payment.domain.enums.PaymentKind.RENTAL)
                    .orElseThrow(() -> new AssertionError("Payment not found for rental: " + rentalId));
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);

            payRental(rentalId);
            payment = paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rentalId, com.akif.payment.domain.enums.PaymentKind.RENTAL)
                    .orElseThrow(() -> new AssertionError("Payment not found for rental: " + rentalId));
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
            moveStartToToday(rentalId);
            mockMvc.perform(post("/api/v1/rentals/{id}/pickup", rentalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andExpect(jsonPath("$.status").value(RentalStatus.IN_USE.name()));

            payment = paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rentalId, com.akif.payment.domain.enums.PaymentKind.RENTAL)
                    .orElseThrow(() -> new AssertionError("Payment not found for rental: " + rentalId));
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);

            mockMvc.perform(post("/api/v1/rentals/{id}/return", rentalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andExpect(jsonPath("$.status").value(RentalStatus.RETURNED.name()));

            Long carId = testCar.getId();
            Car updatedCar = carRepository.findById(carId)
                    .orElseThrow(() -> new AssertionError("Car not found: " + carId));
            assertThat(updatedCar.isAvailable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Email Event Verification Tests")
    class EmailEventVerificationTests {

        @Test
        @DisplayName("Should publish RentalConfirmedEvent when rental is confirmed")
        void shouldPublishRentalConfirmedEventOnConfirm() throws Exception {
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

            eventCaptor.clear();

            mockMvc.perform(post("/api/v1/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            List<RentalConfirmedEvent> confirmedEvents = eventCaptor.getEventsOfType(RentalConfirmedEvent.class);
            assertThat(confirmedEvents).hasSize(1);

            RentalConfirmedEvent event = confirmedEvents.get(0);
            assertThat(event.rentalId()).isEqualTo(rentalId);
            assertThat(event.customerEmail()).isEqualTo(testUser.getEmail());
            assertThat(event.carBrand()).isEqualTo(testCar.getBrand());
            assertThat(event.carModel()).isEqualTo(testCar.getModel());
            assertThat(event.pickupDate()).isEqualTo(rentalRequest.startDate());
            assertThat(event.returnDate()).isEqualTo(rentalRequest.endDate());
            assertThat(event.totalPrice()).isNotNull();
            assertThat(event.currency()).isEqualTo(TestFixtures.DEFAULT_CURRENCY);
        }

        @Test
        @DisplayName("Should publish PaymentCapturedEvent when rental is picked up")
        void shouldPublishPaymentCapturedEventOnPickup() throws Exception {
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
                    .andExpect(status().isOk());

            eventCaptor.clear();

            payRental(rentalId);
            moveStartToToday(rentalId);
            mockMvc.perform(post("/api/v1/rentals/{id}/pickup", rentalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());

            List<PaymentCapturedEvent> capturedEvents = eventCaptor.getEventsOfType(PaymentCapturedEvent.class);
            assertThat(capturedEvents).hasSize(1);

            PaymentCapturedEvent event = capturedEvents.get(0);
            assertThat(event.rentalId()).isEqualTo(rentalId);
            assertThat(event.customerEmail()).isEqualTo(testUser.getEmail());
            assertThat(event.amount()).isNotNull();
            assertThat(event.currency()).isEqualTo(TestFixtures.DEFAULT_CURRENCY);
            assertThat(event.paymentId()).isNotNull();
            assertThat(event.transactionId()).isNotNull();
        }
    }
}
