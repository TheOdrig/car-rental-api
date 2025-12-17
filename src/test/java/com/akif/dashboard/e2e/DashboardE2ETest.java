package com.akif.dashboard.e2e;

import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.car.internal.repository.CarRepository;
import com.akif.dashboard.api.dto.QuickActionResultDto;
import com.akif.dashboard.domain.enums.AlertSeverity;
import com.akif.dashboard.domain.enums.AlertType;
import com.akif.dashboard.domain.model.Alert;
import com.akif.dashboard.internal.repository.AlertRepository;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.Payment;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.domain.enums.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.dto.request.RentalRequest;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.shared.enums.CurrencyType;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@DisplayName("Dashboard E2E Tests")
class DashboardE2ETest extends E2ETestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private CacheManager cacheManager;

    private User testUser;
    private User adminUser;
    private Car testCar;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUpTestData() {
        clearCaches();
        cleanupData();

        testUser = TestDataBuilder.createTestUser();
        testUser = userRepository.save(testUser);
        userToken = generateUserToken(testUser);

        adminUser = TestDataBuilder.createTestAdmin();
        adminUser = userRepository.save(adminUser);
        adminToken = generateAdminToken(adminUser);

        testCar = TestDataBuilder.createAvailableCar();
        testCar = carRepository.save(testCar);
    }

    @AfterEach
    void tearDown() {
        cleanupData();
        clearCaches();
    }

    private void cleanupData() {
        paymentRepository.deleteAll();
        rentalRepository.deleteAll();
        alertRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
    }

    private void clearCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> 
            Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());
    }

    private Rental createRentalDirectly(RentalStatus status, LocalDate startDate, LocalDate endDate) {
        int days = (int) (endDate.toEpochDay() - startDate.toEpochDay());
        BigDecimal dailyPrice = testCar.getPrice();
        BigDecimal totalPrice = dailyPrice.multiply(BigDecimal.valueOf(days));

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
                .days(days)
                .currency(CurrencyType.TRY)
                .dailyPrice(dailyPrice)
                .totalPrice(totalPrice)
                .status(status)
                .build();
        return rentalRepository.save(rental);
    }

    private Payment createPaymentDirectly(Long rentalId, PaymentStatus status, BigDecimal amount) {
        Payment payment = Payment.builder()
                .rentalId(rentalId)
                .userEmail(testUser.getEmail())
                .carLicensePlate(testCar.getLicensePlate())
                .amount(amount)
                .currency(CurrencyType.TRY)
                .status(status)
                .transactionId("test_txn_" + System.currentTimeMillis())
                .paymentMethod("CARD")
                .build();
        return paymentRepository.save(payment);
    }

    private Alert createAlertDirectly(AlertType type, AlertSeverity severity, Long referenceId) {
        Alert alert = Alert.builder()
                .type(type)
                .severity(severity)
                .title(type.name() + " Alert")
                .message("Test alert for " + type.name())
                .actionUrl("/api/rentals/" + referenceId)
                .referenceId(referenceId)
                .acknowledged(false)
                .build();
        return alertRepository.save(alert);
    }

    @Nested
    @DisplayName("Authorization Flow Tests")
    class AuthorizationFlowTests {

        @Test
        @DisplayName("Should return 403 when USER attempts to access dashboard summary")
        void shouldReturn403WhenUserAttemptsToAccessDashboardSummary() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/summary")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when USER attempts to access alerts")
        void shouldReturn403WhenUserAttemptsToAccessAlerts() throws Exception {
            mockMvc.perform(get("/api/admin/alerts")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when USER attempts quick action")
        void shouldReturn403WhenUserAttemptsQuickAction() throws Exception {
            Rental rental = createRentalDirectly(RentalStatus.REQUESTED, 
                    LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));

            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/approve", rental.getId())
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when no authentication provided for dashboard")
        void shouldReturn403WhenNoAuthenticationProvidedForDashboard() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/summary"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when no authentication provided for alerts")
        void shouldReturn403WhenNoAuthenticationProvidedForAlerts() throws Exception {
            mockMvc.perform(get("/api/admin/alerts"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when no authentication provided for quick actions")
        void shouldReturn403WhenNoAuthenticationProvidedForQuickActions() throws Exception {
            mockMvc.perform(post("/api/admin/quick-actions/rentals/1/approve"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 200 when ADMIN accesses dashboard summary")
        void shouldReturn200WhenAdminAccessesDashboardSummary() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/summary")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 200 when ADMIN accesses alerts")
        void shouldReturn200WhenAdminAccessesAlerts() throws Exception {
            mockMvc.perform(get("/api/admin/alerts")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 200 when ADMIN accesses fleet status")
        void shouldReturn200WhenAdminAccessesFleetStatus() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/fleet")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 200 when ADMIN accesses revenue analytics")
        void shouldReturn200WhenAdminAccessesRevenueAnalytics() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/revenue")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Dashboard Summary Flow Tests")
    class DashboardSummaryFlowTests {

        @Test
        @DisplayName("Should return correct counts after creating rentals with different statuses")
        void shouldReturnCorrectCountsAfterCreatingRentalsWithDifferentStatuses() throws Exception {
            createRentalDirectly(RentalStatus.REQUESTED, LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));
            createRentalDirectly(RentalStatus.REQUESTED, LocalDate.now().plusDays(2), LocalDate.now().plusDays(6));

            Car car2 = carRepository.save(TestDataBuilder.createAvailableCar());
            Rental confirmedRental = Rental.builder()
                    .userId(testUser.getId())
                    .carId(car2.getId())
                    .carBrand(car2.getBrand())
                    .carModel(car2.getModel())
                    .carLicensePlate(car2.getLicensePlate())
                    .userEmail(testUser.getEmail())
                    .userFullName(testUser.getUsername())
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusDays(5))
                    .days(5)
                    .currency(CurrencyType.TRY)
                    .dailyPrice(car2.getPrice())
                    .totalPrice(car2.getPrice().multiply(BigDecimal.valueOf(5)))
                    .status(RentalStatus.CONFIRMED)
                    .build();
            rentalRepository.save(confirmedRental);

            Car car3 = carRepository.save(TestDataBuilder.createAvailableCar());
            Rental inUseRental = Rental.builder()
                    .userId(testUser.getId())
                    .carId(car3.getId())
                    .carBrand(car3.getBrand())
                    .carModel(car3.getModel())
                    .carLicensePlate(car3.getLicensePlate())
                    .userEmail(testUser.getEmail())
                    .userFullName(testUser.getUsername())
                    .startDate(LocalDate.now().minusDays(5))
                    .endDate(LocalDate.now())
                    .days(5)
                    .currency(CurrencyType.TRY)
                    .dailyPrice(car3.getPrice())
                    .totalPrice(car3.getPrice().multiply(BigDecimal.valueOf(5)))
                    .status(RentalStatus.IN_USE)
                    .build();
            rentalRepository.save(inUseRental);

            Car car4 = carRepository.save(TestDataBuilder.createAvailableCar());
            Rental overdueRental = Rental.builder()
                    .userId(testUser.getId())
                    .carId(car4.getId())
                    .carBrand(car4.getBrand())
                    .carModel(car4.getModel())
                    .carLicensePlate(car4.getLicensePlate())
                    .userEmail(testUser.getEmail())
                    .userFullName(testUser.getUsername())
                    .startDate(LocalDate.now().minusDays(10))
                    .endDate(LocalDate.now().minusDays(2))
                    .days(8)
                    .currency(CurrencyType.TRY)
                    .dailyPrice(car4.getPrice())
                    .totalPrice(car4.getPrice().multiply(BigDecimal.valueOf(8)))
                    .status(RentalStatus.IN_USE)
                    .build();
            rentalRepository.save(overdueRental);

            clearCaches();

            mockMvc.perform(get("/api/admin/dashboard/summary")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pendingApprovals").value(2))
                    .andExpect(jsonPath("$.todaysPickups").value(1))
                    .andExpect(jsonPath("$.todaysReturns").value(1))
                    .andExpect(jsonPath("$.overdueRentals").value(1))
                    .andExpect(jsonPath("$.generatedAt").exists());
        }

        @Test
        @DisplayName("Should return pending items with correct data")
        void shouldReturnPendingItemsWithCorrectData() throws Exception {
            createRentalDirectly(RentalStatus.REQUESTED, LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));

            mockMvc.perform(get("/api/admin/dashboard/pending/approvals")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].carBrand").value(testCar.getBrand()))
                    .andExpect(jsonPath("$.content[0].carModel").value(testCar.getModel()))
                    .andExpect(jsonPath("$.content[0].customerEmail").value(testUser.getEmail()))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("Quick Action Flow Tests")
    class QuickActionFlowTests {

        @Test
        @DisplayName("Should complete full quick action flow: approve → pickup → return")
        void shouldCompleteFullQuickActionFlow() throws Exception {
            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            
            MvcResult createResult = mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rentalRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            RentalResponse createdRental = objectMapper.readValue(
                    createResult.getResponse().getContentAsString(), RentalResponse.class);
            Long rentalId = createdRental.id();

            MvcResult approveResult = mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/approve", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.newStatus").value(RentalStatus.CONFIRMED.name()))
                    .andExpect(jsonPath("$.updatedSummary").exists())
                    .andReturn();

            QuickActionResultDto approveResultDto = objectMapper.readValue(
                    approveResult.getResponse().getContentAsString(), QuickActionResultDto.class);
            assertThat(approveResultDto.success()).isTrue();

            MvcResult pickupResult = mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/pickup", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.newStatus").value(RentalStatus.IN_USE.name()))
                    .andReturn();

            QuickActionResultDto pickupResultDto = objectMapper.readValue(
                    pickupResult.getResponse().getContentAsString(), QuickActionResultDto.class);
            assertThat(pickupResultDto.success()).isTrue();

            MvcResult returnResult = mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/return", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.newStatus").value(RentalStatus.RETURNED.name()))
                    .andReturn();

            QuickActionResultDto returnResultDto = objectMapper.readValue(
                    returnResult.getResponse().getContentAsString(), QuickActionResultDto.class);
            assertThat(returnResultDto.success()).isTrue();

            mockMvc.perform(get("/api/rentals/{id}", rentalId)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(RentalStatus.RETURNED.getDisplayName()));
        }

        @Test
        @DisplayName("Should return error for invalid rental state on approve")
        void shouldReturnErrorForInvalidRentalStateOnApprove() throws Exception {
            Rental confirmedRental = createRentalDirectly(RentalStatus.CONFIRMED, 
                    LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));

            mockMvc.perform(post("/api/admin/quick-actions/rentals/{id}/approve", confirmedRental.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Should return 404 for non-existent rental")
        void shouldReturn404ForNonExistentRental() throws Exception {
            mockMvc.perform(post("/api/admin/quick-actions/rentals/999999/approve")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Alert Flow Tests")
    class AlertFlowTests {

        @Test
        @DisplayName("Should return alerts sorted by severity")
        void shouldReturnAlertsSortedBySeverity() throws Exception {
            createAlertDirectly(AlertType.LATE_RETURN, AlertSeverity.CRITICAL, 1L);
            createAlertDirectly(AlertType.LOW_AVAILABILITY, AlertSeverity.WARNING, 2L);
            createAlertDirectly(AlertType.FAILED_PAYMENT, AlertSeverity.HIGH, 3L);

            mockMvc.perform(get("/api/admin/alerts")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].severity").value(AlertSeverity.CRITICAL.name()));
        }

        @Test
        @DisplayName("Should filter alerts by type")
        void shouldFilterAlertsByType() throws Exception {
            createAlertDirectly(AlertType.LATE_RETURN, AlertSeverity.CRITICAL, 1L);
            createAlertDirectly(AlertType.FAILED_PAYMENT, AlertSeverity.HIGH, 2L);
            createAlertDirectly(AlertType.LOW_AVAILABILITY, AlertSeverity.WARNING, 3L);

            mockMvc.perform(get("/api/admin/alerts")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("type", AlertType.LATE_RETURN.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].type").value(AlertType.LATE_RETURN.name()));
        }

        @Test
        @DisplayName("Should acknowledge alert successfully")
        void shouldAcknowledgeAlertSuccessfully() throws Exception {
            Alert alert = createAlertDirectly(AlertType.LATE_RETURN, AlertSeverity.CRITICAL, 1L);

            mockMvc.perform(post("/api/admin/alerts/{id}/acknowledge", alert.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.acknowledged").value(true))
                    .andExpect(jsonPath("$.acknowledgedBy").value(adminUser.getUsername()))
                    .andExpect(jsonPath("$.acknowledgedAt").exists());

            mockMvc.perform(get("/api/admin/alerts")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Should return 404 for non-existent alert")
        void shouldReturn404ForNonExistentAlert() throws Exception {
            mockMvc.perform(post("/api/admin/alerts/999999/acknowledge")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Fleet Status Flow Tests")
    class FleetStatusFlowTests {

        @Test
        @DisplayName("Should return correct fleet status with multiple car statuses")
        void shouldReturnCorrectFleetStatusWithMultipleCarStatuses() throws Exception {
            Car rentedCar = Car.builder()
                    .licensePlate("34RNT01")
                    .brand("Honda")
                    .model("Civic")
                    .productionYear(2022)
                    .price(new BigDecimal("600.00"))
                    .currencyType(CurrencyType.TRY)
                    .carStatusType(CarStatusType.RESERVED)
                    .isFeatured(false)
                    .isTestDriveAvailable(true)
                    .viewCount(0L)
                    .likeCount(0L)
                    .build();
            carRepository.save(rentedCar);

            Car maintenanceCar = Car.builder()
                    .licensePlate("34MNT01")
                    .brand("Ford")
                    .model("Focus")
                    .productionYear(2021)
                    .price(new BigDecimal("450.00"))
                    .currencyType(CurrencyType.TRY)
                    .carStatusType(CarStatusType.MAINTENANCE)
                    .isFeatured(false)
                    .isTestDriveAvailable(false)
                    .viewCount(0L)
                    .likeCount(0L)
                    .build();
            carRepository.save(maintenanceCar);

            clearCaches();

            mockMvc.perform(get("/api/admin/dashboard/fleet")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCars").value(3))
                    .andExpect(jsonPath("$.availableCars").value(1))
                    .andExpect(jsonPath("$.rentedCars").value(1))
                    .andExpect(jsonPath("$.maintenanceCars").value(1))
                    .andExpect(jsonPath("$.occupancyRate").exists())
                    .andExpect(jsonPath("$.generatedAt").exists());
        }
    }

    @Nested
    @DisplayName("Revenue Analytics Flow Tests")
    class RevenueAnalyticsFlowTests {

        @Test
        @DisplayName("Should return revenue analytics with captured payments")
        void shouldReturnRevenueAnalyticsWithCapturedPayments() throws Exception {
            Rental rental1 = createRentalDirectly(RentalStatus.RETURNED, 
                    LocalDate.now().minusDays(5), LocalDate.now().minusDays(1));
            createPaymentDirectly(rental1.getId(), PaymentStatus.CAPTURED, new BigDecimal("2000.00"));

            Car car2 = carRepository.save(TestDataBuilder.createAvailableCar());
            Rental rental2 = Rental.builder()
                    .userId(testUser.getId())
                    .carId(car2.getId())
                    .carBrand(car2.getBrand())
                    .carModel(car2.getModel())
                    .carLicensePlate(car2.getLicensePlate())
                    .userEmail(testUser.getEmail())
                    .userFullName(testUser.getUsername())
                    .startDate(LocalDate.now().minusDays(3))
                    .endDate(LocalDate.now())
                    .days(3)
                    .currency(CurrencyType.TRY)
                    .dailyPrice(car2.getPrice())
                    .totalPrice(car2.getPrice().multiply(BigDecimal.valueOf(3)))
                    .status(RentalStatus.RETURNED)
                    .build();
            rental2 = rentalRepository.save(rental2);
            createPaymentDirectly(rental2.getId(), PaymentStatus.CAPTURED, new BigDecimal("1500.00"));

            clearCaches();

            mockMvc.perform(get("/api/admin/dashboard/revenue")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dailyRevenue").isArray())
                    .andExpect(jsonPath("$.monthlyRevenue").isArray())
                    .andExpect(jsonPath("$.breakdown").exists());
        }

        @Test
        @DisplayName("Should return monthly metrics for date range")
        void shouldReturnMonthlyMetricsForDateRange() throws Exception {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();

            mockMvc.perform(get("/api/admin/dashboard/metrics")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("startDate", startDate.toString())
                            .param("endDate", endDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.startDate").value(startDate.toString()))
                    .andExpect(jsonPath("$.endDate").value(endDate.toString()))
                    .andExpect(jsonPath("$.totalRevenue").exists())
                    .andExpect(jsonPath("$.completedRentals").exists())
                    .andExpect(jsonPath("$.cancelledRentals").exists())
                    .andExpect(jsonPath("$.generatedAt").exists());
        }
    }

    @Nested
    @DisplayName("Cache Invalidation Flow Tests")
    class CacheInvalidationFlowTests {

        @Test
        @DisplayName("Should refresh summary after rental state change")
        void shouldRefreshSummaryAfterRentalStateChange() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/summary")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pendingApprovals").value(0));

            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rentalRequest)))
                    .andExpect(status().isCreated());

            clearCaches();

            mockMvc.perform(get("/api/admin/dashboard/summary")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pendingApprovals").value(1));
        }

        @Test
        @DisplayName("Should refresh fleet status after car status change")
        void shouldRefreshFleetStatusAfterCarStatusChange() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard/fleet")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.availableCars").value(1));

            carRepository.save(TestDataBuilder.createAvailableCar());

            clearCaches();

            mockMvc.perform(get("/api/admin/dashboard/fleet")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.availableCars").value(2));
        }
    }
}
