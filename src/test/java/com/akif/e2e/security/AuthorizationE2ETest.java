package com.akif.e2e.security;

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
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@DisplayName("Authorization and Security E2E Tests")
class AuthorizationE2ETest extends E2ETestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Nested
    @DisplayName("USER Role Admin Operation Tests")
    class UserRoleAdminOperationTests {

        @Test
        @DisplayName("Should return 403 when USER attempts to confirm rental")
        void shouldReturn403WhenUserAttemptsConfirm() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when USER attempts to pickup rental")
        void shouldReturn403WhenUserAttemptsPickup() throws Exception {
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

            mockMvc.perform(post("/api/rentals/{id}/pickup", rentalId)
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when USER attempts to return rental")
        void shouldReturn403WhenUserAttemptsReturn() throws Exception {
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

            mockMvc.perform(post("/api/rentals/{id}/pickup", rentalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/rentals/{id}/return", rentalId)
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Unauthenticated Access Tests")
    class UnauthenticatedAccessTests {

        @Test
        @DisplayName("Should return 401/403 when accessing rental request without token")
        void shouldReturn401Or403WhenAccessingRentalRequestWithoutToken() throws Exception {
            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());

            mockMvc.perform(post("/api/rentals/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rentalRequest)))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Should return 401/403 when accessing confirm without token")
        void shouldReturn401Or403WhenAccessingConfirmWithoutToken() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Should return 401/403 when accessing pickup without token")
        void shouldReturn401Or403WhenAccessingPickupWithoutToken() throws Exception {
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

            mockMvc.perform(post("/api/rentals/{id}/pickup", rentalId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Should return 401/403 when accessing return without token")
        void shouldReturn401Or403WhenAccessingReturnWithoutToken() throws Exception {
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

            mockMvc.perform(post("/api/rentals/{id}/pickup", rentalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/rentals/{id}/return", rentalId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("Cross-User Cancellation Tests")
    class CrossUserCancellationTests {

        @Test
        @DisplayName("Should return 403 when user attempts to cancel another user's rental")
        void shouldReturn403WhenUserCancelsAnotherUsersRental() throws Exception {
            User testUser1 = TestDataBuilder.createTestUser("user1");
            testUser1 = userRepository.save(testUser1);
            String user1Token = generateUserToken(testUser1);

            User testUser2 = TestDataBuilder.createTestUser("user2");
            testUser2 = userRepository.save(testUser2);
            String user2Token = generateUserToken(testUser2);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, user1Token);

            mockMvc.perform(post("/api/rentals/{id}/cancel", rentalId)
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Cross-User View Tests")
    class CrossUserViewTests {

        @Test
        @DisplayName("Should return 403 when user attempts to view another user's rental details")
        void shouldReturn403WhenUserViewsAnotherUsersRental() throws Exception {
            User testUser1 = TestDataBuilder.createTestUser("user1");
            testUser1 = userRepository.save(testUser1);
            String user1Token = generateUserToken(testUser1);

            User testUser2 = TestDataBuilder.createTestUser("user2");
            testUser2 = userRepository.save(testUser2);
            String user2Token = generateUserToken(testUser2);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, user1Token);

            mockMvc.perform(get("/api/rentals/{id}", rentalId)
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isForbidden());
        }
    }
}
