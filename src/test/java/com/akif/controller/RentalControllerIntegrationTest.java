package com.akif.controller;

import com.akif.dto.request.RentalRequestDto;
import com.akif.shared.enums.CarStatusType;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.Role;
import com.akif.model.Car;
import com.akif.model.User;
import com.akif.repository.CarRepository;
import com.akif.repository.UserRepository;
import com.akif.shared.security.JwtTokenProvider;
import com.akif.starter.CarGalleryProjectApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("RentalController Integration Tests")
class RentalControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private MockMvc mockMvc;

    private User testUser;
    private User adminUser;
    private Car testCar;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();
        testUser = userRepository.save(testUser);

        adminUser = User.builder()
                .username("admin")
                .email("admin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .roles(Set.of(Role.USER, Role.ADMIN))
                .enabled(true)
                .build();
        adminUser = userRepository.save(adminUser);

        testCar = Car.builder()
                .licensePlate("34ABC123")
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2020)
                .price(new BigDecimal("500.00"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .isFeatured(false)
                .isTestDriveAvailable(true)
                .viewCount(0L)
                .likeCount(0L)
                .build();
        testCar = carRepository.save(testCar);

        Authentication userAuth = new UsernamePasswordAuthenticationToken(
                testUser.getUsername(), null,
                testUser.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .collect(Collectors.toList())
        );
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                adminUser.getUsername(), null,
                adminUser.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .collect(Collectors.toList())
        );
        
        userToken = "Bearer " + tokenProvider.generateAccessToken(userAuth);
        adminToken = "Bearer " + tokenProvider.generateAccessToken(adminAuth);
    }

    @Nested
    @DisplayName("requestRental Tests")
    class RequestRentalTests {

        @Test
        @DisplayName("Should create rental request successfully")
        void shouldCreateRentalRequestSuccessfully() throws Exception {
            RentalRequestDto request = RentalRequestDto.builder()
                    .carId(testCar.getId())
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .notes("Test rental request")
                    .build();

            mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.status").value("Requested"));
        }

        @Test
        @DisplayName("Should return 403 when unauthenticated")
        void shouldReturn403WhenUnauthenticated() throws Exception {
            RentalRequestDto request = RentalRequestDto.builder()
                    .carId(testCar.getId())
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .build();

            mockMvc.perform(post("/api/rentals/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 400 when car not available")
        void shouldReturn400WhenCarNotAvailable() throws Exception {
            testCar.setCarStatusType(CarStatusType.SOLD);
            carRepository.save(testCar);

            RentalRequestDto request = RentalRequestDto.builder()
                    .carId(testCar.getId())
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .build();

            mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Complete Rental Workflow Tests")
    class CompleteRentalWorkflowTests {

        @Test
        @DisplayName("Should complete full rental workflow: request -> confirm -> pickup -> return")
        void shouldCompleteFullRentalWorkflow() throws Exception {
            RentalRequestDto request = RentalRequestDto.builder()
                    .carId(testCar.getId())
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .build();

            String rentalResponse = mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("Requested"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long rentalId = objectMapper.readTree(rentalResponse).get("id").asLong();

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("Confirmed"));

            mockMvc.perform(post("/api/rentals/{id}/pickup", rentalId)
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"notes\": \"Car picked up\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("In Use"));

            mockMvc.perform(post("/api/rentals/{id}/return", rentalId)
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"notes\": \"Car returned\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("Returned"));
        }

        @Test
        @DisplayName("Should complete cancellation workflow: request -> cancel")
        void shouldCompleteCancellationWorkflow() throws Exception {
            RentalRequestDto request = RentalRequestDto.builder()
                    .carId(testCar.getId())
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .build();

            String rentalResponse = mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long rentalId = objectMapper.readTree(rentalResponse).get("id").asLong();

            mockMvc.perform(post("/api/rentals/{id}/cancel", rentalId)
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("Cancelled"));
        }

        @Test
        @DisplayName("Should complete refund workflow: request -> confirm -> cancel")
        void shouldCompleteRefundWorkflow() throws Exception {
            RentalRequestDto request = RentalRequestDto.builder()
                    .carId(testCar.getId())
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .build();

            String rentalResponse = mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long rentalId = objectMapper.readTree(rentalResponse).get("id").asLong();

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/rentals/{id}/cancel", rentalId)
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("Cancelled"));
        }
    }

    @Nested
    @DisplayName("getMyRentals Tests")
    class GetMyRentalsTests {

        @Test
        @DisplayName("Should return user's rentals")
        void shouldReturnUsersRentals() throws Exception {
            mockMvc.perform(get("/api/rentals/me")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should return 403 when user tries to access admin endpoint")
        void shouldReturn403WhenUserTriesToAccessAdminEndpoint() throws Exception {
            RentalRequestDto request = RentalRequestDto.builder()
                    .carId(testCar.getId())
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .build();

            String rentalResponse = mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long rentalId = objectMapper.readTree(rentalResponse).get("id").asLong();

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Date Overlap Tests")
    class DateOverlapTests {

        @Test
        @DisplayName("Should prevent overlapping confirmed rentals")
        void shouldPreventOverlappingConfirmedRentals() throws Exception {
            RentalRequestDto request1 = RentalRequestDto.builder()
                    .carId(testCar.getId())
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .build();

            String rentalResponse = mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long rentalId = objectMapper.readTree(rentalResponse).get("id").asLong();

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk());

            RentalRequestDto request2 = RentalRequestDto.builder()
                    .carId(testCar.getId())
                    .startDate(LocalDate.now().plusDays(3))
                    .endDate(LocalDate.now().plusDays(7))
                    .build();

            mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should allow multiple REQUESTED rentals for same dates")
        void shouldAllowMultipleRequestedRentalsForSameDates() throws Exception {
            RentalRequestDto request1 = RentalRequestDto.builder()
                    .carId(testCar.getId())
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .build();

            mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated());

            RentalRequestDto request2 = RentalRequestDto.builder()
                    .carId(testCar.getId())
                    .startDate(LocalDate.now().plusDays(3))
                    .endDate(LocalDate.now().plusDays(7))
                    .build();

            mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isCreated());
        }
    }
}
