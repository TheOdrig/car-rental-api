package com.akif.e2e.edgecases;

import com.akif.dto.request.RentalRequestDto;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.model.Car;
import com.akif.model.User;
import com.akif.repository.CarRepository;
import com.akif.repository.UserRepository;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@DisplayName("Concurrency E2E Tests")
class ConcurrencyE2ETest extends E2ETestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Nested
    @DisplayName("Concurrent Rental Confirmation Tests")
    class ConcurrentRentalConfirmationTests {

        @Test
        @DisplayName("Should allow only one rental confirmation when multiple REQUESTED rentals exist for same car and dates")
        void shouldAllowOnlyOneConfirmation_whenMultipleRequestedRentalsExist() throws Exception {
            User testAdmin = TestDataBuilder.createTestAdmin();
            testAdmin = userRepository.save(testAdmin);
            String adminToken = generateAdminToken(testAdmin);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            LocalDate startDate = LocalDate.now().plusDays(10);
            LocalDate endDate = LocalDate.now().plusDays(15);
            
            List<Long> rentalIds = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                User testUser = TestDataBuilder.createTestUser("user" + i);
                testUser = userRepository.save(testUser);
                String userToken = generateUserToken(testUser);
                
                RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(
                        testCar.getId(), startDate, endDate);
                Long rentalId = createAndGetRentalId(rentalRequest, userToken);
                rentalIds.add(rentalId);
            }

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalIds.get(0))
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalIds.get(1))
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isConflict());

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalIds.get(2))
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Idempotent Confirmation Tests")
    class IdempotentConfirmationTests {

        @Test
        @DisplayName("Should prevent duplicate payment by rejecting second confirmation attempt")
        void shouldPreventDuplicatePayment_byRejectingSecondConfirmation() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            User testAdmin = TestDataBuilder.createTestAdmin();
            testAdmin = userRepository.save(testAdmin);
            String adminToken = generateAdminToken(testAdmin);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            LocalDate startDate = LocalDate.now().plusDays(10);
            LocalDate endDate = LocalDate.now().plusDays(15);
            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(
                    testCar.getId(), startDate, endDate);
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_RENTAL_STATE"));
        }

        @Test
        @DisplayName("Should validate rental state and reject confirmation of already confirmed rental")
        void shouldRejectConfirmation_whenRentalAlreadyConfirmed() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            User testAdmin = TestDataBuilder.createTestAdmin();
            testAdmin = userRepository.save(testAdmin);
            String adminToken = generateAdminToken(testAdmin);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            LocalDate startDate = LocalDate.now().plusDays(10);
            LocalDate endDate = LocalDate.now().plusDays(15);
            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(
                    testCar.getId(), startDate, endDate);
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("Confirmed"));

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_RENTAL_STATE"))
                    .andExpect(jsonPath("$.message").value("Invalid rental state. Current: CONFIRMED, Required: REQUESTED"));
        }
    }
}
