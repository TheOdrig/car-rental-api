package com.akif.e2e.edgecases;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.rental.api.RentalRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.akif.testsupport.TestcontainersConfiguration;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@DisplayName("Date Overlap and Availability E2E Tests")
class DateOverlapE2ETest extends E2ETestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Nested
    @DisplayName("Date Overlap Prevention Tests")
    class DateOverlapPreventionTests {

        @Test
        @DisplayName("Should reject rental request when dates overlap with confirmed rental")
        void shouldRejectRentalRequest_whenDatesOverlapWithConfirmedRental() throws Exception {
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
            RentalRequest firstRentalRequest = TestDataBuilder.createRentalRequest(
                    testCar.getId(), startDate, endDate);

            Long firstRentalId = createAndGetRentalId(firstRentalRequest, userToken);

            mockMvc.perform(post("/api/v1/rentals/{id}/confirm", firstRentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            LocalDate overlappingStart = LocalDate.now().plusDays(12);
            LocalDate overlappingEnd = LocalDate.now().plusDays(17);
            RentalRequest overlappingRentalRequest = TestDataBuilder.createRentalRequest(
                    testCar.getId(), overlappingStart, overlappingEnd);

            mockMvc.perform(post("/api/v1/rentals/request")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(overlappingRentalRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("CAR_NOT_AVAILABLE"));
        }
    }

    @Nested
    @DisplayName("Availability After Cancellation Tests")
    class AvailabilityAfterCancellationTests {

        @Test
        @DisplayName("Should allow rental for same dates after cancellation")
        void shouldAllowRentalForSameDates_afterCancellation() throws Exception {
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
            RentalRequest firstRentalRequest = TestDataBuilder.createRentalRequest(
                    testCar.getId(), startDate, endDate);

            Long firstRentalId = createAndGetRentalId(firstRentalRequest, userToken);

            mockMvc.perform(post("/api/v1/rentals/{id}/confirm", firstRentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/rentals/{id}/cancel", firstRentalId)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk());

            RentalRequest secondRentalRequest = TestDataBuilder.createRentalRequest(
                    testCar.getId(), startDate, endDate);

            mockMvc.perform(post("/api/v1/rentals/request")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(secondRentalRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.status").value("Requested"));
        }
    }

    @Nested
    @DisplayName("Pending Request Holds The Dates Tests")
    class PendingRequestHoldsDatesTests {

        @Test
        @DisplayName("Should reject a second request for the same dates while the first is still REQUESTED")
        void shouldRejectSecondRequest_whileFirstIsPending() throws Exception {
            User testUser1 = TestDataBuilder.createTestUser("user1");
            testUser1 = userRepository.save(testUser1);
            String userToken1 = generateUserToken(testUser1);

            User testUser2 = TestDataBuilder.createTestUser("user2");
            testUser2 = userRepository.save(testUser2);
            String userToken2 = generateUserToken(testUser2);

            User testAdmin = TestDataBuilder.createTestAdmin();
            testAdmin = userRepository.save(testAdmin);
            String adminToken = generateAdminToken(testAdmin);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            LocalDate startDate = LocalDate.now().plusDays(10);
            LocalDate endDate = LocalDate.now().plusDays(15);

            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(
                    testCar.getId(), startDate, endDate);
            Long firstRentalId = createAndGetRentalId(rentalRequest, userToken1);

            mockMvc.perform(post("/api/v1/rentals/request")
                            .header("Authorization", "Bearer " + userToken2)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rentalRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("CAR_NOT_AVAILABLE"));

            mockMvc.perform(post("/api/v1/rentals/{id}/confirm", firstRentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("Confirmed"));
        }
    }
}
