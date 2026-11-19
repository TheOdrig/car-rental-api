package com.akif.rental.integration;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.api.CarStatusType;
import com.akif.car.internal.repository.CarRepository;
import com.akif.rental.api.RentalRequest;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.Role;
import com.akif.shared.security.JwtTokenService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.akif.testsupport.TestcontainersConfiguration;
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

import static java.util.UUID.randomUUID;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("RentalController Integration Tests")
class RentalControllerIntegrationTest {

    @Autowired
    private com.akif.rental.internal.repository.RentalRepository rentalRepository;

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private com.akif.payment.api.PaymentService paymentService;

        private void payRental(Long rentalId) {
                paymentService.createOrGetIntentForRental(rentalId);
        }

        @Autowired
        private CarRepository carRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private JwtTokenService tokenProvider;

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

                String uniqueId = randomUUID().toString().substring(0, 6);

                testUser = User.builder()
                                .username("testuser_" + uniqueId)
                                .email("test_" + uniqueId + "@example.com")
                                .password(passwordEncoder.encode("password123"))
                                .roles(new java.util.HashSet<>(Set.of(Role.USER)))
                                .enabled(true)
                                .build();
                testUser = userRepository.save(testUser);

                adminUser = User.builder()
                                .username("admin_" + uniqueId)
                                .email("admin_" + uniqueId + "@example.com")
                                .password(passwordEncoder.encode("admin123"))
                                .roles(new java.util.HashSet<>(Set.of(Role.USER, Role.ADMIN)))
                                .enabled(true)
                                .build();
                adminUser = userRepository.save(adminUser);

                testCar = Car.builder()
                                .licensePlate("34ABC" + uniqueId)
                                .brand("Toyota")
                                .model("Corolla")
                                .productionYear(2020)
                                .price(new BigDecimal("500.00"))
                                .currencyType(CurrencyType.TRY)
                                .carStatusType(CarStatusType.AVAILABLE)
                                .build();
                testCar = carRepository.save(testCar);

                Authentication userAuth = new UsernamePasswordAuthenticationToken(
                                testUser.getUsername(), null,
                                testUser.getRoles().stream()
                                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                                                .collect(Collectors.toList()));
                Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                                adminUser.getUsername(), null,
                                adminUser.getRoles().stream()
                                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                                                .collect(Collectors.toList()));

                userToken = "Bearer " + tokenProvider.generateAccessToken(userAuth, testUser.getId());
                adminToken = "Bearer " + tokenProvider.generateAccessToken(adminAuth, adminUser.getId());
        }

        @Nested
        @DisplayName("requestRental Tests")
        class RequestRentalTests {

                @Test
                @DisplayName("Should create rental request successfully")
                void shouldCreateRentalRequestSuccessfully() throws Exception {
                        RentalRequest request = new RentalRequest(
                                        testCar.getId(),
                                        LocalDate.now().plusDays(1),
                                        LocalDate.now().plusDays(5),
                                        "Test rental request");

                        mockMvc.perform(post("/api/v1/rentals/request")
                                        .header("Authorization", userToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.id").exists())
                                        .andExpect(jsonPath("$.userSummary.username").value(testUser.getUsername()))
                                        .andExpect(jsonPath("$.userSummary.email").value(testUser.getEmail()))
                                        .andExpect(jsonPath("$.status").value("REQUESTED"));
                }

                @Test
                @DisplayName("Should return 403 when unauthenticated")
                void shouldReturn403WhenUnauthenticated() throws Exception {
                        RentalRequest request = new RentalRequest(
                                        testCar.getId(),
                                        LocalDate.now().plusDays(1),
                                        LocalDate.now().plusDays(5),
                                        null);

                        mockMvc.perform(post("/api/v1/rentals/request")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isUnauthorized());
                }

                @Test
                @DisplayName("Should return 400 when car not available")
                void shouldReturn400WhenCarNotAvailable() throws Exception {
                        testCar.setCarStatusType(CarStatusType.MAINTENANCE);
                        carRepository.save(testCar);

                        RentalRequest request = new RentalRequest(
                                        testCar.getId(),
                                        LocalDate.now().plusDays(1),
                                        LocalDate.now().plusDays(5),
                                        null);

                        mockMvc.perform(post("/api/v1/rentals/request")
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
                        RentalRequest request = new RentalRequest(
                                        testCar.getId(),
                                        LocalDate.now().plusDays(1),
                                        LocalDate.now().plusDays(5),
                                        null);

                        String rentalResponse = mockMvc.perform(post("/api/v1/rentals/request")
                                        .header("Authorization", userToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.status").value("REQUESTED"))
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString();

                        Long rentalId = objectMapper.readTree(rentalResponse).get("id").asLong();

                        mockMvc.perform(post("/api/v1/rentals/{id}/confirm", rentalId)
                                        .header("Authorization", adminToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.status").value("CONFIRMED"))
                                        .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                                        .andExpect(jsonPath("$.paymentDueBy").exists());

                        payRental(rentalId);
                        mockMvc.perform(get("/api/v1/rentals/{id}", rentalId)
                                        .header("Authorization", adminToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.paymentStatus").value("AUTHORIZED"))
                                        .andExpect(jsonPath("$.paymentDueBy").doesNotExist());
                        startToday(rentalId);
                        mockMvc.perform(post("/api/v1/rentals/{id}/pickup", rentalId)
                                        .header("Authorization", adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"notes\": \"Car picked up\"}"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.status").value("IN_USE"));

                        mockMvc.perform(post("/api/v1/rentals/{id}/return", rentalId)
                                        .header("Authorization", adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"notes\": \"Car returned\"}"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.status").value("RETURNED"));
                }

                @Test
                @DisplayName("Should complete cancellation workflow: request -> cancel")
                void shouldCompleteCancellationWorkflow() throws Exception {
                        RentalRequest request = new RentalRequest(
                                        testCar.getId(),
                                        LocalDate.now().plusDays(1),
                                        LocalDate.now().plusDays(5),
                                        null);

                        String rentalResponse = mockMvc.perform(post("/api/v1/rentals/request")
                                        .header("Authorization", userToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString();

                        Long rentalId = objectMapper.readTree(rentalResponse).get("id").asLong();

                        mockMvc.perform(post("/api/v1/rentals/{id}/cancel", rentalId)
                                        .header("Authorization", userToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.status").value("CANCELLED"));
                }

                @Test
                @DisplayName("Should complete refund workflow: request -> confirm -> cancel")
                void shouldCompleteRefundWorkflow() throws Exception {
                        RentalRequest request = new RentalRequest(
                                        testCar.getId(),
                                        LocalDate.now().plusDays(1),
                                        LocalDate.now().plusDays(5),
                                        null);

                        String rentalResponse = mockMvc.perform(post("/api/v1/rentals/request")
                                        .header("Authorization", userToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString();

                        Long rentalId = objectMapper.readTree(rentalResponse).get("id").asLong();

                        mockMvc.perform(post("/api/v1/rentals/{id}/confirm", rentalId)
                                        .header("Authorization", adminToken))
                                        .andExpect(status().isOk());

                        mockMvc.perform(post("/api/v1/rentals/{id}/cancel", rentalId)
                                        .header("Authorization", userToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.status").value("CANCELLED"));
                }
        }

        @Nested
        @DisplayName("getMyRentals Tests")
        class GetMyRentalsTests {

                @Test
                @DisplayName("Should return user's rentals")
                void shouldReturnUsersRentals() throws Exception {
                        mockMvc.perform(get("/api/v1/rentals/me")
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
                        RentalRequest request = new RentalRequest(
                                        testCar.getId(),
                                        LocalDate.now().plusDays(1),
                                        LocalDate.now().plusDays(5),
                                        null);

                        String rentalResponse = mockMvc.perform(post("/api/v1/rentals/request")
                                        .header("Authorization", userToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString();

                        Long rentalId = objectMapper.readTree(rentalResponse).get("id").asLong();

                        mockMvc.perform(post("/api/v1/rentals/{id}/confirm", rentalId)
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
                        RentalRequest request1 = new RentalRequest(
                                        testCar.getId(),
                                        LocalDate.now().plusDays(1),
                                        LocalDate.now().plusDays(5),
                                        null);

                        String rentalResponse = mockMvc.perform(post("/api/v1/rentals/request")
                                        .header("Authorization", userToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request1)))
                                        .andExpect(status().isCreated())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString();

                        Long rentalId = objectMapper.readTree(rentalResponse).get("id").asLong();

                        mockMvc.perform(post("/api/v1/rentals/{id}/confirm", rentalId)
                                        .header("Authorization", adminToken))
                                        .andExpect(status().isOk());

                        RentalRequest request2 = new RentalRequest(
                                        testCar.getId(),
                                        LocalDate.now().plusDays(3),
                                        LocalDate.now().plusDays(7),
                                        null);

                        mockMvc.perform(post("/api/v1/rentals/request")
                                        .header("Authorization", userToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request2)))
                                        .andExpect(status().isConflict())
                                        .andExpect(jsonPath("$.code").value("CAR_DATES_UNAVAILABLE"));
                }

                @Test
                @DisplayName("Should reject a second REQUESTED rental for overlapping dates")
                void shouldRejectSecondRequestedRentalForOverlappingDates() throws Exception {
                        RentalRequest request1 = new RentalRequest(
                                        testCar.getId(),
                                        LocalDate.now().plusDays(1),
                                        LocalDate.now().plusDays(5),
                                        null);

                        mockMvc.perform(post("/api/v1/rentals/request")
                                        .header("Authorization", userToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request1)))
                                        .andExpect(status().isCreated());

                        RentalRequest request2 = new RentalRequest(
                                        testCar.getId(),
                                        LocalDate.now().plusDays(3),
                                        LocalDate.now().plusDays(7),
                                        null);

                        mockMvc.perform(post("/api/v1/rentals/request")
                                        .header("Authorization", userToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request2)))
                                        .andExpect(status().isConflict())
                                        .andExpect(jsonPath("$.code").value("CAR_DATES_UNAVAILABLE"));
                }
        }

        @Nested
        @DisplayName("Admin status filter Tests")
        class AdminStatusFilterTests {

                private Long requestRental(int fromDays, int toDays) throws Exception {
                        RentalRequest request = new RentalRequest(
                                        testCar.getId(),
                                        LocalDate.now().plusDays(fromDays),
                                        LocalDate.now().plusDays(toDays),
                                        null);
                        String body = mockMvc.perform(post("/api/v1/rentals/request")
                                        .header("Authorization", userToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated())
                                        .andReturn().getResponse().getContentAsString();
                        return objectMapper.readTree(body).get("id").asLong();
                }

                private java.util.List<String> idsAndStatuses(String body) throws Exception {
                        java.util.List<String> out = new java.util.ArrayList<>();
                        for (tools.jackson.databind.JsonNode n : objectMapper.readTree(body).get("content")) {
                                out.add(n.get("id").asLong() + ":" + n.get("status").asText());
                        }
                        return out;
                }

                private String adminList(String... statusValues) throws Exception {
                        var request = get("/api/v1/rentals/admin")
                                        .header("Authorization", adminToken)
                                        .param("size", "100");
                        if (statusValues.length > 0) {
                                request.param("status", statusValues);
                        }
                        return mockMvc.perform(request)
                                        .andExpect(status().isOk())
                                        .andReturn().getResponse().getContentAsString();
                }

                @Test
                @DisplayName("Should filter by one status code, a repeated parameter and a comma list")
                void shouldFilterByStatus() throws Exception {
                        Long requested = requestRental(1, 3);
                        Long confirmed = requestRental(10, 12);
                        mockMvc.perform(post("/api/v1/rentals/{id}/confirm", confirmed)
                                        .header("Authorization", adminToken))
                                        .andExpect(status().isOk());

                        org.assertj.core.api.Assertions.assertThat(idsAndStatuses(adminList("CONFIRMED")))
                                        .contains(confirmed + ":CONFIRMED")
                                        .doesNotContain(requested + ":REQUESTED")
                                        .allMatch(s -> s.endsWith(":CONFIRMED"));
                        org.assertj.core.api.Assertions.assertThat(idsAndStatuses(adminList("CONFIRMED", "REQUESTED")))
                                        .contains(confirmed + ":CONFIRMED", requested + ":REQUESTED");
                        org.assertj.core.api.Assertions.assertThat(idsAndStatuses(adminList("CONFIRMED,REQUESTED")))
                                        .contains(confirmed + ":CONFIRMED", requested + ":REQUESTED");
                        org.assertj.core.api.Assertions.assertThat(idsAndStatuses(adminList()))
                                        .contains(confirmed + ":CONFIRMED", requested + ":REQUESTED");
                }

                @Test
                @DisplayName("Should answer 400 INVALID_PARAMETER for an unknown status")
                void shouldRejectUnknownStatus() throws Exception {
                        mockMvc.perform(get("/api/v1/rentals/admin")
                                        .header("Authorization", adminToken)
                                        .param("status", "CONFIRMED,NOPE"))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
                }

                @Test
                @DisplayName("Should keep the paging and sort guards in force with a status filter")
                void shouldKeepPagingGuards() throws Exception {
                        requestRental(1, 3);
                        mockMvc.perform(get("/api/v1/rentals/admin")
                                        .header("Authorization", adminToken)
                                        .param("status", "REQUESTED")
                                        .param("sort", "passwordHash,asc"))
                                        .andExpect(status().isBadRequest());
                        mockMvc.perform(get("/api/v1/rentals/admin")
                                        .header("Authorization", adminToken)
                                        .param("status", "REQUESTED")
                                        .param("sort", "startDate,desc")
                                        .param("size", "1"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.content.length()").value(1));
                }

                @Test
                @DisplayName("Should deny a non-admin")
                void shouldDenyNonAdmin() throws Exception {
                        mockMvc.perform(get("/api/v1/rentals/admin")
                                        .header("Authorization", userToken)
                                        .param("status", "REQUESTED"))
                                        .andExpect(status().isForbidden());
                }
        }

    private void startToday(Long rentalId) {
        com.akif.rental.domain.model.Rental rental = rentalRepository.findById(rentalId).orElseThrow();
        org.springframework.test.util.ReflectionTestUtils.setField(rental, "startDate", LocalDate.now());
        rentalRepository.saveAndFlush(rental);
    }

}
