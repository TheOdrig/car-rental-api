package com.akif.e2e.rental;

import com.akif.dto.request.RentalRequestDto;
import com.akif.dto.response.RentalResponseDto;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.e2e.infrastructure.TestFixtures;
import com.akif.shared.enums.PaymentStatus;
import com.akif.shared.enums.RentalStatus;
import com.akif.event.PaymentCapturedEvent;
import com.akif.event.RentalConfirmedEvent;
import com.akif.model.Car;
import com.akif.model.Payment;
import com.akif.model.User;
import com.akif.repository.CarRepository;
import com.akif.repository.PaymentRepository;
import com.akif.repository.UserRepository;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
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

            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());

            String responseJson = mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rentalRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.status").value(RentalStatus.REQUESTED.getDisplayName()))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RentalResponseDto rentalResponse = objectMapper.readValue(responseJson, RentalResponseDto.class);
            Long rentalId = rentalResponse.getId();
            assertThat(rentalId).isNotNull();

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andExpect(jsonPath("$.status").value(RentalStatus.CONFIRMED.getDisplayName()));

            Payment payment = paymentRepository.findByRentalIdAndIsDeletedFalse(rentalId)
                    .orElseThrow(() -> new AssertionError("Payment not found for rental: " + rentalId));
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);

            mockMvc.perform(post("/api/rentals/{id}/pickup", rentalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andExpect(jsonPath("$.status").value(RentalStatus.IN_USE.getDisplayName()));

            payment = paymentRepository.findByRentalIdAndIsDeletedFalse(rentalId)
                    .orElseThrow(() -> new AssertionError("Payment not found for rental: " + rentalId));
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);

            mockMvc.perform(post("/api/rentals/{id}/return", rentalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andExpect(jsonPath("$.status").value(RentalStatus.RETURNED.getDisplayName()));

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

            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());

            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            eventCaptor.clear();

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            List<RentalConfirmedEvent> confirmedEvents = eventCaptor.getEventsOfType(RentalConfirmedEvent.class);
            assertThat(confirmedEvents).hasSize(1);

            RentalConfirmedEvent event = confirmedEvents.get(0);
            assertThat(event.getRentalId()).isEqualTo(rentalId);
            assertThat(event.getCustomerEmail()).isEqualTo(testUser.getEmail());
            assertThat(event.getCarBrand()).isEqualTo(testCar.getBrand());
            assertThat(event.getCarModel()).isEqualTo(testCar.getModel());
            assertThat(event.getPickupDate()).isEqualTo(rentalRequest.getStartDate());
            assertThat(event.getReturnDate()).isEqualTo(rentalRequest.getEndDate());
            assertThat(event.getTotalPrice()).isNotNull();
            assertThat(event.getCurrency()).isEqualTo(TestFixtures.DEFAULT_CURRENCY);
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

            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());

            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            eventCaptor.clear();

            mockMvc.perform(post("/api/rentals/{id}/pickup", rentalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());

            List<PaymentCapturedEvent> capturedEvents = eventCaptor.getEventsOfType(PaymentCapturedEvent.class);
            assertThat(capturedEvents).hasSize(1);

            PaymentCapturedEvent event = capturedEvents.get(0);
            assertThat(event.getRentalId()).isEqualTo(rentalId);
            assertThat(event.getCustomerEmail()).isEqualTo(testUser.getEmail());
            assertThat(event.getAmount()).isNotNull();
            assertThat(event.getCurrency()).isEqualTo(TestFixtures.DEFAULT_CURRENCY);
            assertThat(event.getPaymentId()).isNotNull();
            assertThat(event.getTransactionId()).isNotNull();
        }
    }
}
