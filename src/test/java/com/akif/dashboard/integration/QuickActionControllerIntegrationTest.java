package com.akif.dashboard.integration;

import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.car.internal.repository.CarRepository;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.rental.domain.enums.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.Role;
import com.akif.shared.security.JwtTokenProvider;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("QuickActionController Integration Tests")
class QuickActionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private PaymentRepository paymentRepository;

    private User testUser;
    private User adminUser;
    private Car testCar;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(cacheName -> 
            Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());

        paymentRepository.deleteAll();
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("quickactionuser")
                .email("quickaction@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();
        testUser = userRepository.save(testUser);

        adminUser = User.builder()
                .username("quickactionadmin")
                .email("quickactionadmin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .roles(Set.of(Role.USER, Role.ADMIN))
                .enabled(true)
                .build();
        adminUser = userRepository.save(adminUser);

        testCar = Car.builder()
                .licensePlate("34QCK001")
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

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();

        cacheManager.getCacheNames().forEach(cacheName -> 
            Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());
    }

    private Rental createTestRental(RentalStatus status, CarStatusType carStatus) {
        testCar.setCarStatusType(carStatus);
        testCar = carRepository.save(testCar);

        Rental rental = Rental.builder()
                .userId(testUser.getId())
                .carId(testCar.getId())
                .carBrand(testCar.getBrand())
                .carModel(testCar.getModel())
                .carLicensePlate(testCar.getLicensePlate())
                .userEmail(testUser.getEmail())
                .userFullName(testUser.getUsername())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(5))
                .days(5)
                .currency(CurrencyType.TRY)
                .dailyPrice(new BigDecimal("500.00"))
                .totalPrice(new BigDecimal("2500.00"))
                .status(status)
                .build();
        return rentalRepository.save(rental);
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should return 403 when user tries to approve rental")
        void shouldReturn403WhenUserTriesToApproveRental() throws Exception {
            Rental rental = createTestRental(RentalStatus.REQUESTED, CarStatusType.AVAILABLE);

            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/approve", rental.getId())
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when no authentication provided")
        void shouldReturn403WhenNoAuthenticationProvided() throws Exception {
            Rental rental = createTestRental(RentalStatus.REQUESTED, CarStatusType.AVAILABLE);

            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/approve", rental.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when user tries to process pickup")
        void shouldReturn403WhenUserTriesToProcessPickup() throws Exception {
            Rental rental = createTestRental(RentalStatus.CONFIRMED, CarStatusType.RESERVED);

            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/pickup", rental.getId())
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when user tries to process return")
        void shouldReturn403WhenUserTriesToProcessReturn() throws Exception {
            Rental rental = createTestRental(RentalStatus.IN_USE, CarStatusType.RESERVED);

            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/return", rental.getId())
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Approve Rental Tests")
    class ApproveRentalTests {

        @Test
        @DisplayName("Should approve rental successfully")
        void shouldApproveRentalSuccessfully() throws Exception {
            Rental rental = createTestRental(RentalStatus.REQUESTED, CarStatusType.AVAILABLE);

            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/approve", rental.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.newStatus").value("CONFIRMED"))
                    .andExpect(jsonPath("$.updatedSummary").exists());

            Rental updatedRental = rentalRepository.findById(rental.getId()).orElseThrow();
            assertThat(updatedRental.getStatus()).isEqualTo(RentalStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Should return 404 when rental not found")
        void shouldReturn404WhenRentalNotFound() throws Exception {
            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/approve", 99999L)
                            .header("Authorization", adminToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when rental is not in REQUESTED state")
        void shouldReturn400WhenRentalIsNotInRequestedState() throws Exception {
            Rental rental = createTestRental(RentalStatus.CONFIRMED, CarStatusType.RESERVED);

            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/approve", rental.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Process Pickup Tests")
    class ProcessPickupTests {

        @Test
        @DisplayName("Should process pickup successfully")
        void shouldProcessPickupSuccessfully() throws Exception {
            Rental rental = createTestRental(RentalStatus.REQUESTED, CarStatusType.AVAILABLE);

            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/approve", rental.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/pickup", rental.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.newStatus").value("IN_USE"))
                    .andExpect(jsonPath("$.updatedSummary").exists());

            Rental updatedRental = rentalRepository.findById(rental.getId()).orElseThrow();
            assertThat(updatedRental.getStatus()).isEqualTo(RentalStatus.IN_USE);
        }

        @Test
        @DisplayName("Should return 404 when rental not found")
        void shouldReturn404WhenRentalNotFound() throws Exception {
            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/pickup", 99999L)
                            .header("Authorization", adminToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when rental is not in CONFIRMED state")
        void shouldReturn400WhenRentalIsNotInConfirmedState() throws Exception {
            Rental rental = createTestRental(RentalStatus.REQUESTED, CarStatusType.AVAILABLE);

            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/pickup", rental.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Process Return Tests")
    class ProcessReturnTests {

        @Test
        @DisplayName("Should process return successfully")
        void shouldProcessReturnSuccessfully() throws Exception {
            Rental rental = createTestRental(RentalStatus.REQUESTED, CarStatusType.AVAILABLE);

            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/approve", rental.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/pickup", rental.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/return", rental.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.newStatus").value("RETURNED"))
                    .andExpect(jsonPath("$.updatedSummary").exists());

            Rental updatedRental = rentalRepository.findById(rental.getId()).orElseThrow();
            assertThat(updatedRental.getStatus()).isEqualTo(RentalStatus.RETURNED);
        }

        @Test
        @DisplayName("Should return 404 when rental not found")
        void shouldReturn404WhenRentalNotFound() throws Exception {
            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/return", 99999L)
                            .header("Authorization", adminToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when rental is not in IN_USE state")
        void shouldReturn400WhenRentalIsNotInInUseState() throws Exception {
            Rental rental = createTestRental(RentalStatus.CONFIRMED, CarStatusType.RESERVED);

            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/return", rental.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Complete Workflow Tests")
    class CompleteWorkflowTests {

        @Test
        @DisplayName("Should complete full quick action workflow: approve -> pickup -> return")
        void shouldCompleteFullQuickActionWorkflow() throws Exception {
            Rental rental = createTestRental(RentalStatus.REQUESTED, CarStatusType.AVAILABLE);

            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/approve", rental.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.newStatus").value("CONFIRMED"));

            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/pickup", rental.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.newStatus").value("IN_USE"));

            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/return", rental.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.newStatus").value("RETURNED"));

            Rental finalRental = rentalRepository.findById(rental.getId()).orElseThrow();
            assertThat(finalRental.getStatus()).isEqualTo(RentalStatus.RETURNED);
        }
    }
}
