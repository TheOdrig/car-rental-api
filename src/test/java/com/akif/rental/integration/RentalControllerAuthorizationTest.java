package com.akif.rental.integration;

import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.car.internal.repository.CarRepository;
import com.akif.rental.internal.dto.request.RentalRequest;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.Role;
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
@DisplayName("RentalController Authorization Integration Tests")
class RentalControllerAuthorizationTest {

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
                .username("testuser_auth")
                .email("testauth@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();
        testUser = userRepository.save(testUser);

        adminUser = User.builder()
                .username("admin_auth")
                .email("adminauth@example.com")
                .password(passwordEncoder.encode("admin123"))
                .roles(Set.of(Role.USER, Role.ADMIN))
                .enabled(true)
                .build();
        adminUser = userRepository.save(adminUser);

        testCar = Car.builder()
                .licensePlate("34AUTH123")
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
        
        userToken = "Bearer " + tokenProvider.generateAccessToken(userAuth, testUser.getId());
        adminToken = "Bearer " + tokenProvider.generateAccessToken(adminAuth, adminUser.getId());
    }

    @Nested
    @DisplayName("Admin Operations - Should Allow ADMIN Role")
    class AdminOperationsAllowed {

        @Test
        @DisplayName("POST /api/rentals/{id}/confirm - Admin should be allowed")
        void adminShouldConfirmRental() throws Exception {
            Long rentalId = createRentalRequest();

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("Confirmed"));
        }

        @Test
        @DisplayName("POST /api/rentals/{id}/pickup - Admin should be allowed")
        void adminShouldPickupRental() throws Exception {
            Long rentalId = createAndConfirmRental();

            mockMvc.perform(post("/api/rentals/{id}/pickup", rentalId)
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"notes\": \"Car picked up\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("In Use"));
        }

        @Test
        @DisplayName("POST /api/rentals/{id}/return - Admin should be allowed")
        void adminShouldReturnRental() throws Exception {
            Long rentalId = createConfirmAndPickupRental();

            mockMvc.perform(post("/api/rentals/{id}/return", rentalId)
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"notes\": \"Car returned\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("Returned"));
        }
    }

    @Nested
    @DisplayName("Admin Operations - Should Deny USER Role")
    class AdminOperationsDenied {

        @Test
        @DisplayName("POST /api/rentals/{id}/confirm - User should be denied (403)")
        void userShouldBeDeniedConfirmRental() throws Exception {
            Long rentalId = createRentalRequest();

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /api/rentals/{id}/pickup - User should be denied (403)")
        void userShouldBeDeniedPickupRental() throws Exception {
            Long rentalId = createAndConfirmRental();

            mockMvc.perform(post("/api/rentals/{id}/pickup", rentalId)
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"notes\": \"Car picked up\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /api/rentals/{id}/return - User should be denied (403)")
        void userShouldBeDeniedReturnRental() throws Exception {
            Long rentalId = createConfirmAndPickupRental();

            mockMvc.perform(post("/api/rentals/{id}/return", rentalId)
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"notes\": \"Car returned\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("User Operations - Should Allow USER Role")
    class UserOperationsAllowed {

        @Test
        @DisplayName("POST /api/rentals/request - User should be allowed")
        void userShouldRequestRental() throws Exception {
            RentalRequest request = new RentalRequest(
                    testCar.getId(),
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(5),
                    "Test rental"
            );

            mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("GET /api/rentals/me - User should be allowed")
        void userShouldGetOwnRentals() throws Exception {
            mockMvc.perform(get("/api/rentals/me")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("POST /api/rentals/{id}/cancel - User should be allowed to cancel own rental")
        void userShouldCancelOwnRental() throws Exception {
            Long rentalId = createRentalRequest();

            mockMvc.perform(post("/api/rentals/{id}/cancel", rentalId)
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("Cancelled"));
        }
    }

    private Long createRentalRequest() throws Exception {
        RentalRequest request = new RentalRequest(
                testCar.getId(),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(5),
                null
        );

        String response = mockMvc.perform(post("/api/rentals/request")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createAndConfirmRental() throws Exception {
        Long rentalId = createRentalRequest();

        mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());

        return rentalId;
    }

    private Long createConfirmAndPickupRental() throws Exception {
        Long rentalId = createAndConfirmRental();

        mockMvc.perform(post("/api/rentals/{id}/pickup", rentalId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\": \"Car picked up\"}"))
                .andExpect(status().isOk());

        return rentalId;
    }
}
