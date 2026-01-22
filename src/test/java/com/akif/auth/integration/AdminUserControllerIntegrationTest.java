package com.akif.auth.integration;

import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.car.internal.repository.CarRepository;
import com.akif.damage.domain.enums.DamageCategory;
import com.akif.damage.domain.enums.DamageSeverity;
import com.akif.damage.domain.enums.DamageStatus;
import com.akif.damage.domain.model.DamageReport;
import com.akif.damage.internal.repository.DamageReportRepository;
import com.akif.rental.domain.enums.LateReturnStatus;
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
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AdminUserController Integration Tests")
class AdminUserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private DamageReportRepository damageReportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private CacheManager cacheManager;

    private User testCustomer;
    private User adminUser;
    private Car testCar;
    private String customerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames()
                .forEach(cacheName -> Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());

        damageReportRepository.deleteAll();
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();

        testCustomer = User.builder()
                .username("testcustomer")
                .email("customer@example.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("John")
                .lastName("Customer")
                .phone("+1234567890")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();
        testCustomer = userRepository.save(testCustomer);

        adminUser = User.builder()
                .username("adminuser")
                .email("admin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .firstName("Admin")
                .lastName("User")
                .roles(Set.of(Role.USER, Role.ADMIN))
                .enabled(true)
                .build();
        adminUser = userRepository.save(adminUser);

        testCar = Car.builder()
                .licensePlate("34ADM001")
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

        Authentication customerAuth = new UsernamePasswordAuthenticationToken(
                testCustomer.getUsername(), null,
                testCustomer.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .collect(Collectors.toList()));
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                adminUser.getUsername(), null,
                adminUser.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .collect(Collectors.toList()));

        customerToken = "Bearer " + tokenProvider.generateAccessToken(customerAuth, testCustomer.getId());
        adminToken = "Bearer " + tokenProvider.generateAccessToken(adminAuth, adminUser.getId());
    }

    @AfterEach
    void tearDown() {
        damageReportRepository.deleteAll();
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();

        cacheManager.getCacheNames()
                .forEach(cacheName -> Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());
    }

    private Rental createTestRental(User user, RentalStatus status, boolean isLate) {
        Rental rental = Rental.builder()
                .userId(user.getId())
                .carId(testCar.getId())
                .carBrand(testCar.getBrand())
                .carModel(testCar.getModel())
                .carLicensePlate(testCar.getLicensePlate())
                .userEmail(user.getEmail())
                .userFullName(user.getFirstName() + " " + user.getLastName())
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(5))
                .days(5)
                .currency(CurrencyType.TRY)
                .dailyPrice(new BigDecimal("500.00"))
                .totalPrice(new BigDecimal("2500.00"))
                .status(status)
                .lateReturnStatus(isLate ? LateReturnStatus.LATE : LateReturnStatus.ON_TIME)
                .build();
        return rentalRepository.save(rental);
    }

    private DamageReport createTestDamageReport(User user, Rental rental) {
        DamageReport damage = DamageReport.builder()
                .rentalId(rental.getId())
                .carId(testCar.getId())
                .carBrand(testCar.getBrand())
                .carModel(testCar.getModel())
                .carLicensePlate(testCar.getLicensePlate())
                .rentalStartDate(rental.getStartDate())
                .rentalEndDate(rental.getEndDate())
                .customerEmail(user.getEmail())
                .customerFullName(user.getFirstName() + " " + user.getLastName())
                .customerUserId(user.getId())
                .description("Test damage")
                .damageLocation("Front bumper")
                .severity(DamageSeverity.MINOR)
                .category(DamageCategory.SCRATCH)
                .status(DamageStatus.RESOLVED)
                .repairCostEstimate(new BigDecimal("200.00"))
                .customerLiability(new BigDecimal("150.00"))
                .reportedBy(adminUser.getId())
                .reportedAt(LocalDateTime.now())
                .build();
        return damageReportRepository.save(damage);
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should return 403 when customer tries to access admin user detail")
        void shouldReturn403WhenCustomerTriesToAccessAdminUserDetail() throws Exception {
            mockMvc.perform(get("/api/admin/users/{id}", testCustomer.getId())
                    .header("Authorization", customerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when no authentication provided")
        void shouldReturn403WhenNoAuthenticationProvided() throws Exception {
            mockMvc.perform(get("/api/admin/users/{id}", testCustomer.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 200 when admin accesses user detail")
        void shouldReturn200WhenAdminAccessesUserDetail() throws Exception {
            mockMvc.perform(get("/api/admin/users/{id}", testCustomer.getId())
                    .header("Authorization", adminToken))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Get User Detail Tests")
    class GetUserDetailTests {

        @Test
        @DisplayName("Should return complete user details with all fields")
        void shouldReturnCompleteUserDetailsWithAllFields() throws Exception {
            mockMvc.perform(get("/api/admin/users/{id}", testCustomer.getId())
                    .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testCustomer.getId()))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Customer"))
                    .andExpect(jsonPath("$.email").value("customer@example.com"))
                    .andExpect(jsonPath("$.phone").value("+1234567890"))
                    .andExpect(jsonPath("$.verification").exists())
                    .andExpect(jsonPath("$.statistics").exists())
                    .andExpect(jsonPath("$.accountStatus").exists());
        }

        @Test
        @DisplayName("Should return 404 when user does not exist")
        void shouldReturn404WhenUserDoesNotExist() throws Exception {
            mockMvc.perform(get("/api/admin/users/{id}", 999999L)
                    .header("Authorization", adminToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return verification info in response")
        void shouldReturnVerificationInfoInResponse() throws Exception {
            mockMvc.perform(get("/api/admin/users/{id}", testCustomer.getId())
                    .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verification.emailVerified").value(true))
                    .andExpect(jsonPath("$.verification.phoneVerified").value(true));
        }

        @Test
        @DisplayName("Should return account status as ACTIVE for enabled user")
        void shouldReturnAccountStatusAsActiveForEnabledUser() throws Exception {
            mockMvc.perform(get("/api/admin/users/{id}", testCustomer.getId())
                    .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountStatus.status").value("ACTIVE"));
        }
    }

    @Nested
    @DisplayName("Statistics Calculation Tests")
    class StatisticsCalculationTests {

        @Test
        @DisplayName("Should return zero statistics for user with no rentals")
        void shouldReturnZeroStatisticsForUserWithNoRentals() throws Exception {
            mockMvc.perform(get("/api/admin/users/{id}", testCustomer.getId())
                    .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statistics.totalRentals").value(0))
                    .andExpect(jsonPath("$.statistics.completedRentals").value(0))
                    .andExpect(jsonPath("$.statistics.cancelledRentals").value(0))
                    .andExpect(jsonPath("$.statistics.activeRentals").value(0))
                    .andExpect(jsonPath("$.statistics.totalDamageReports").value(0));
        }

        @Test
        @DisplayName("Should return correct statistics with rental data")
        void shouldReturnCorrectStatisticsWithRentalData() throws Exception {
            createTestRental(testCustomer, RentalStatus.RETURNED, false);
            createTestRental(testCustomer, RentalStatus.RETURNED, true);
            createTestRental(testCustomer, RentalStatus.CANCELLED, false);

            mockMvc.perform(get("/api/admin/users/{id}", testCustomer.getId())
                    .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statistics.totalRentals").value(3))
                    .andExpect(jsonPath("$.statistics.completedRentals").value(2))
                    .andExpect(jsonPath("$.statistics.cancelledRentals").value(1))
                    .andExpect(jsonPath("$.statistics.lateReturns").value(1));
        }

        @Test
        @DisplayName("Should return correct damage statistics")
        void shouldReturnCorrectDamageStatistics() throws Exception {
            Rental rental = createTestRental(testCustomer, RentalStatus.RETURNED, false);
            createTestDamageReport(testCustomer, rental);
            createTestDamageReport(testCustomer, rental);

            mockMvc.perform(get("/api/admin/users/{id}", testCustomer.getId())
                    .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statistics.totalDamageReports").value(2))
                    .andExpect(jsonPath("$.statistics.totalDamageCost").value(300.00));
        }
    }
}
