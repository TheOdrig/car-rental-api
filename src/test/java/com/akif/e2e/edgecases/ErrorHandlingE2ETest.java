package com.akif.e2e.edgecases;

import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.car.internal.exception.CarNotAvailableException;
import com.akif.car.internal.exception.CarNotFoundException;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import com.akif.rental.domain.enums.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.dto.request.PickupRequest;
import com.akif.rental.internal.dto.request.RentalRequest;
import com.akif.rental.internal.exception.InvalidRentalStateException;
import com.akif.payment.internal.exception.PaymentFailedException;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@DisplayName("Error Handling E2E Tests")
class ErrorHandlingE2ETest extends E2ETestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @MockitoBean
    private PaymentGateway paymentGateway;

    @Nested
    @DisplayName("Non-Existent Car Tests")
    class NonExistentCarTests {

        @Test
        @DisplayName("Should return HTTP 404 when requesting rental for non-existent car")
        void shouldReturn404WhenCarNotFound() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            Long nonExistentCarId = 999999L;
            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(nonExistentCarId);

            mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rentalRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(CarNotFoundException.ERROR_CODE))
                    .andExpect(jsonPath("$.message").value("Car not found with id: " + nonExistentCarId));
        }
    }

    @Nested
    @DisplayName("Unavailable Car Tests")
    class UnavailableCarTests {

        @Test
        @DisplayName("Should return HTTP 400 when requesting rental for SOLD car")
        void shouldReturn400WhenCarIsSold() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            Car soldCar = TestDataBuilder.createAvailableCar();
            soldCar.setCarStatusType(CarStatusType.SOLD);
            soldCar = carRepository.save(soldCar);

            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(soldCar.getId());

            mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rentalRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(CarNotAvailableException.ERROR_CODE))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should return HTTP 400 when requesting rental for RESERVED car")
        void shouldReturn400WhenCarIsReserved() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            Car reservedCar = TestDataBuilder.createAvailableCar();
            reservedCar.setCarStatusType(CarStatusType.RESERVED);
            reservedCar = carRepository.save(reservedCar);

            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(reservedCar.getId());

            mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rentalRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(CarNotAvailableException.ERROR_CODE))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("Invalid State Transition Tests")
    class InvalidStateTransitionTests {

        @Test
        @DisplayName("Should return HTTP 400 when attempting to pickup REQUESTED rental")
        void shouldReturn400WhenPickupRequestedRental() throws Exception {
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

            PickupRequest pickupRequest = new PickupRequest(null);

            mockMvc.perform(post("/api/rentals/{id}/pickup", rentalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(pickupRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(InvalidRentalStateException.ERROR_CODE))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should return HTTP 400 when attempting to return REQUESTED rental")
        void shouldReturn400WhenReturnRequestedRental() throws Exception {
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

            mockMvc.perform(post("/api/rentals/{id}/return", rentalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(InvalidRentalStateException.ERROR_CODE))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should return HTTP 400 when attempting to return CONFIRMED rental")
        void shouldReturn400WhenReturnConfirmedRental() throws Exception {
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

            when(paymentGateway.authorize(any(), any(), anyString()))
                    .thenReturn(PaymentResult.success("TEST-AUTH-123", "Authorized"));

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/rentals/{id}/return", rentalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(InvalidRentalStateException.ERROR_CODE))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("Payment Failure Tests")
    class PaymentFailureTests {

        @Test
        @DisplayName("Should keep rental in REQUESTED state when payment authorization fails")
        void shouldKeepRentalRequestedWhenPaymentFails() throws Exception {
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

            when(paymentGateway.authorize(any(), any(), anyString()))
                    .thenReturn(PaymentResult.failure("Payment authorization failed"));

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isPaymentRequired())
                    .andExpect(jsonPath("$.errorCode").value(PaymentFailedException.ERROR_CODE))
                    .andExpect(jsonPath("$.message").exists());

            Rental rental = rentalRepository.findById(rentalId)
                    .orElseThrow(() -> new AssertionError("Rental not found: " + rentalId));
            assertThat(rental.getStatus()).isEqualTo(RentalStatus.REQUESTED);
        }
    }
}
