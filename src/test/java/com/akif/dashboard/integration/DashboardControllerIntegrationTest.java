package com.akif.dashboard.integration;

import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.car.internal.repository.CarRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("DashboardController Integration Tests")
class DashboardControllerIntegrationTest {

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

    private User testUser;
    private User adminUser;
    private Car testCar;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(cacheName -> 
            Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());

        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("dashboardtestuser")
                .email("dashboardtest@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();
        testUser = userRepository.save(testUser);

        adminUser = User.builder()
                .username("dashboardadmin")
                .email("dashboardadmin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .roles(Set.of(Role.USER, Role.ADMIN))
                .enabled(true)
                .build();
        adminUser = userRepository.save(adminUser);

        testCar = Car.builder()
                .licensePlate("34DSH001")
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

    @AfterEach
    void tearDown() {
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();

        cacheManager.getCacheNames().forEach(cacheName -> 
            Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());
    }

    private Rental createTestRental(RentalStatus status, LocalDate startDate, LocalDate endDate) {
        Rental rental = Rental.builder()
                .userId(testUser.getId())
                .carId(testCar.getId())
                .carBrand(testCar.getBrand())
                .carModel(testCar.getModel())
                .carLicensePlate(testCar.getLicensePlate())
                .userEmail(testUser.getEmail())
                .userFullName(testUser.getUsername())
                .startDate(startDate)
                .endDate(endDate)
                .days((int) (endDate.toEpochDay() - startDate.toEpochDay()))
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
        @DisplayName("Should return 403 when user tries to access admin dashboard")
        void shouldReturn403WhenUserTriesToAccessAdminDashboard() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/summary")
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when no authentication provided")
        void shouldReturn403WhenNoAuthenticationProvided() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/summary"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 200 when admin accesses dashboard")
        void shouldReturn200WhenAdminAccessesDashboard() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/summary")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Daily Summary Tests")
    class DailySummaryTests {

        @Test
        @DisplayName("Should return daily summary with zero counts when no data")
        void shouldReturnDailySummaryWithZeroCountsWhenNoData() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/summary")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pendingApprovals").value(0))
                    .andExpect(jsonPath("$.todaysPickups").value(0))
                    .andExpect(jsonPath("$.todaysReturns").value(0))
                    .andExpect(jsonPath("$.overdueRentals").value(0))
                    .andExpect(jsonPath("$.generatedAt").exists());
        }

        @Test
        @DisplayName("Should return correct pending approvals count")
        void shouldReturnCorrectPendingApprovalsCount() throws Exception {
            createTestRental(RentalStatus.REQUESTED, LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));

            mockMvc.perform(get("/api/admin/dashboard/summary")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pendingApprovals").value(1));
        }

        @Test
        @DisplayName("Should return correct todays pickups count")
        void shouldReturnCorrectTodaysPickupsCount() throws Exception {
            createTestRental(RentalStatus.CONFIRMED, LocalDate.now(), LocalDate.now().plusDays(5));

            mockMvc.perform(get("/api/admin/dashboard/summary")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.todaysPickups").value(1));
        }

        @Test
        @DisplayName("Should return correct todays returns count")
        void shouldReturnCorrectTodaysReturnsCount() throws Exception {
            createTestRental(RentalStatus.IN_USE, LocalDate.now().minusDays(5), LocalDate.now());

            mockMvc.perform(get("/api/admin/dashboard/summary")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.todaysReturns").value(1));
        }

        @Test
        @DisplayName("Should return correct overdue rentals count")
        void shouldReturnCorrectOverdueRentalsCount() throws Exception {
            createTestRental(RentalStatus.IN_USE, LocalDate.now().minusDays(10), LocalDate.now().minusDays(1));

            mockMvc.perform(get("/api/admin/dashboard/summary")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.overdueRentals").value(1));
        }
    }

    @Nested
    @DisplayName("Fleet Status Tests")
    class FleetStatusTests {

        @Test
        @DisplayName("Should return fleet status successfully")
        void shouldReturnFleetStatusSuccessfully() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/fleet")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCars").exists())
                    .andExpect(jsonPath("$.availableCars").exists())
                    .andExpect(jsonPath("$.rentedCars").exists())
                    .andExpect(jsonPath("$.maintenanceCars").exists())
                    .andExpect(jsonPath("$.occupancyRate").exists())
                    .andExpect(jsonPath("$.generatedAt").exists());
        }

        @Test
        @DisplayName("Should return 403 when user tries to access fleet status")
        void shouldReturn403WhenUserTriesToAccessFleetStatus() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/fleet")
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Monthly Metrics Tests")
    class MonthlyMetricsTests {

        @Test
        @DisplayName("Should return monthly metrics without date range")
        void shouldReturnMonthlyMetricsWithoutDateRange() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/metrics")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRevenue").exists())
                    .andExpect(jsonPath("$.completedRentals").exists())
                    .andExpect(jsonPath("$.cancelledRentals").exists())
                    .andExpect(jsonPath("$.startDate").exists())
                    .andExpect(jsonPath("$.endDate").exists())
                    .andExpect(jsonPath("$.generatedAt").exists());
        }

        @Test
        @DisplayName("Should return monthly metrics with date range")
        void shouldReturnMonthlyMetricsWithDateRange() throws Exception {
            LocalDate startDate = LocalDate.now().minusMonths(1);
            LocalDate endDate = LocalDate.now();

            mockMvc.perform(get("/api/admin/dashboard/metrics")
                            .header("Authorization", adminToken)
                            .param("startDate", startDate.toString())
                            .param("endDate", endDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.startDate").value(startDate.toString()))
                    .andExpect(jsonPath("$.endDate").value(endDate.toString()));
        }

        @Test
        @DisplayName("Should return 403 when user tries to access metrics")
        void shouldReturn403WhenUserTriesToAccessMetrics() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/metrics")
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Revenue Analytics Tests")
    class RevenueAnalyticsTests {

        @Test
        @DisplayName("Should return revenue analytics successfully")
        void shouldReturnRevenueAnalyticsSuccessfully() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/revenue")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dailyRevenue").exists())
                    .andExpect(jsonPath("$.monthlyRevenue").exists())
                    .andExpect(jsonPath("$.breakdown").exists());
        }

        @Test
        @DisplayName("Should return 403 when user tries to access revenue analytics")
        void shouldReturn403WhenUserTriesToAccessRevenueAnalytics() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/revenue")
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Pending Items Pagination Tests")
    class PendingItemsPaginationTests {

        @Test
        @DisplayName("Should return pending approvals with pagination")
        void shouldReturnPendingApprovalsWithPagination() throws Exception {
            createTestRental(RentalStatus.REQUESTED, LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));

            mockMvc.perform(get("/api/admin/dashboard/pending/approvals")
                            .header("Authorization", adminToken)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.pageable").exists())
                    .andExpect(jsonPath("$.totalElements").exists());
        }

        @Test
        @DisplayName("Should return todays pickups with pagination")
        void shouldReturnTodaysPickupsWithPagination() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/pending/pickups")
                            .header("Authorization", adminToken)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("Should return todays returns with pagination")
        void shouldReturnTodaysReturnsWithPagination() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/pending/returns")
                            .header("Authorization", adminToken)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("Should return overdue rentals with pagination")
        void shouldReturnOverdueRentalsWithPagination() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/pending/overdue")
                            .header("Authorization", adminToken)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("Should return 403 when user tries to access pending items")
        void shouldReturn403WhenUserTriesToAccessPendingItems() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/pending/approvals")
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }
    }
}
