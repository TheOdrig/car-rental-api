package com.akif.rental.integration;

import java.time.Duration;
import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.api.CarStatusType;
import com.akif.car.internal.repository.CarRepository;
import com.akif.notification.internal.service.email.EmailSender;
import com.akif.rental.domain.enums.LateReturnStatus;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.rental.internal.scheduler.LateReturnScheduler;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.akif.testsupport.TestcontainersConfiguration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("LateReturnController Integration Tests")
class LateReturnControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailSender emailSender;

    @MockitoBean
    private LateReturnScheduler lateReturnScheduler;

    private User testUser;
    private Car testCar;
    private Rental lateRental;
    private Rental severelyLateRental;

    @BeforeEach
    void setUp() {
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .isDeleted(false)
                .build();
        testUser = userRepository.save(testUser);

        testCar = Car.builder()
                .licensePlate("34ABC123")
                .vinNumber("1HGBH41JXMN109186")
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2020)
                .price(new BigDecimal("250000"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .build();
        testCar = carRepository.save(testCar);

        lateRental = Rental.builder()
                .userId(testUser.getId())
                .userEmail(testUser.getEmail())
                .userFullName(testUser.getUsername())
                .carId(testCar.getId())
                .carBrand(testCar.getBrand())
                .carModel(testCar.getModel())
                .carLicensePlate(testCar.getLicensePlate())
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().minusDays(2))
                .days(3)
                .currency(CurrencyType.TRY)
                .dailyPrice(new BigDecimal("1000"))
                .totalPrice(new BigDecimal("3000"))
                .status(RentalStatus.IN_USE)
                .lateReturnStatus(LateReturnStatus.LATE)
                .lateDetectedAt(Instant.now().minus(Duration.ofDays(1)))
                .lateHours(30)
                .penaltyAmount(new BigDecimal("1500"))
                .penaltyPaid(false)
                .build();
        lateRental = rentalRepository.save(lateRental);

        severelyLateRental = Rental.builder()
                .userId(testUser.getId())
                .userEmail(testUser.getEmail())
                .userFullName(testUser.getUsername())
                .carId(testCar.getId())
                .carBrand(testCar.getBrand())
                .carModel(testCar.getModel())
                .carLicensePlate(testCar.getLicensePlate())
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(6))
                .days(5)
                .currency(CurrencyType.TRY)
                .dailyPrice(new BigDecimal("1000"))
                .totalPrice(new BigDecimal("5000"))
                .status(RentalStatus.IN_USE)
                .lateReturnStatus(LateReturnStatus.SEVERELY_LATE)
                .lateDetectedAt(Instant.now().minus(Duration.ofDays(4)))
                .lateHours(100)
                .penaltyAmount(new BigDecimal("5000"))
                .penaltyPaid(true)
                .build();
        severelyLateRental = rentalRepository.save(severelyLateRental);
    }

    @AfterEach
    void tearDown() {
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/late-returns - Should return late returns report")
    void shouldReturnLateReturnsReport() throws Exception {
        mockMvc.perform(get("/api/v1/admin/late-returns")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].rentalId").exists())
                .andExpect(jsonPath("$.content[0].customerName").exists())
                .andExpect(jsonPath("$.content[0].lateHours").exists())
                .andExpect(jsonPath("$.content[0].penaltyAmount").exists())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/late-returns - Should filter by date range")
    void shouldFilterByDateRange() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(6);
        LocalDate endDate = LocalDate.now().minusDays(1);

        mockMvc.perform(get("/api/v1/admin/late-returns")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/late-returns - Should filter by status")
    void shouldFilterByStatus() throws Exception {
        mockMvc.perform(get("/api/v1/admin/late-returns")
                        .param("status", "SEVERELY_LATE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/late-returns - Should sort by penalty amount")
    void shouldSortByPenaltyAmount() throws Exception {
        mockMvc.perform(get("/api/v1/admin/late-returns")
                        .param("sortBy", "penaltyAmount")
                        .param("sortDirection", "DESC")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].penaltyAmount").exists())
                .andExpect(jsonPath("$.content[1].penaltyAmount").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/late-returns/statistics - Should return statistics")
    void shouldReturnStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/admin/late-returns/statistics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.totalLateReturns").value(2))
                .andExpect(jsonPath("$.severelyLateCount").value(1))
                .andExpect(jsonPath("$.penaltiesByCurrency[0].totalPenaltyAmount").value(6500))
                .andExpect(jsonPath("$.penaltiesByCurrency[0].collectedPenaltyAmount").value(5000))
                .andExpect(jsonPath("$.penaltiesByCurrency[0].pendingPenaltyAmount").value(1500))
                .andExpect(jsonPath("$.penaltiesByCurrency[0].currency").exists())
                .andExpect(jsonPath("$.averageLateHours").exists())
                .andExpect(jsonPath("$.lateReturnPercentage").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/late-returns/statistics - Should filter statistics by date range")
    void shouldFilterStatisticsByDateRange() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(6);
        LocalDate endDate = LocalDate.now().minusDays(1);

        mockMvc.perform(get("/api/v1/admin/late-returns/statistics")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.totalLateReturns").exists())
                .andExpect(jsonPath("$.penaltiesByCurrency").isArray());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/admin/late-returns - Should deny access for non-admin")
    void shouldDenyAccessForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/late-returns"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/late-returns - Should deny access for unauthenticated")
    void shouldDenyAccessForUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/late-returns"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/admin/late-returns/statistics - Should deny access for non-admin")
    void shouldDenyAccessForNonAdminStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/admin/late-returns/statistics"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/late-returns/statistics - Should deny access for unauthenticated")
    void shouldDenyAccessForUnauthenticatedStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/admin/late-returns/statistics"))
                .andExpect(status().isUnauthorized());
    }
}
