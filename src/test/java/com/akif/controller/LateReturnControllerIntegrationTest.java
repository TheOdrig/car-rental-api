package com.akif.controller;

import com.akif.shared.enums.CarStatusType;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.LateReturnStatus;
import com.akif.shared.enums.RentalStatus;
import com.akif.shared.enums.Role;
import com.akif.model.Car;
import com.akif.model.Rental;
import com.akif.model.User;
import com.akif.repository.CarRepository;
import com.akif.repository.RentalRepository;
import com.akif.repository.UserRepository;
import com.akif.scheduler.LateReturnScheduler;
import com.akif.service.email.IEmailSender;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("LateReturnController Integration Tests")
public class LateReturnControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private IEmailSender emailSender;

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
                .damagePrice(BigDecimal.ZERO)
                .carStatusType(CarStatusType.AVAILABLE)
                .isFeatured(false)
                .isTestDriveAvailable(true)
                .viewCount(0L)
                .likeCount(0L)
                .build();
        testCar = carRepository.save(testCar);

        lateRental = Rental.builder()
                .user(testUser)
                .car(testCar)
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().minusDays(2))
                .days(3)
                .currency(CurrencyType.TRY)
                .dailyPrice(new BigDecimal("1000"))
                .totalPrice(new BigDecimal("3000"))
                .status(RentalStatus.IN_USE)
                .lateReturnStatus(LateReturnStatus.LATE)
                .lateDetectedAt(LocalDateTime.now().minusDays(1))
                .lateHours(30)
                .penaltyAmount(new BigDecimal("1500"))
                .penaltyPaid(false)
                .build();
        lateRental = rentalRepository.save(lateRental);

        severelyLateRental = Rental.builder()
                .user(testUser)
                .car(testCar)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(5))
                .days(5)
                .currency(CurrencyType.TRY)
                .dailyPrice(new BigDecimal("1000"))
                .totalPrice(new BigDecimal("5000"))
                .status(RentalStatus.IN_USE)
                .lateReturnStatus(LateReturnStatus.SEVERELY_LATE)
                .lateDetectedAt(LocalDateTime.now().minusDays(4))
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
    @DisplayName("GET /api/admin/late-returns - Should return late returns report")
    void shouldReturnLateReturnsReport() throws Exception {
        mockMvc.perform(get("/api/admin/late-returns")
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
    @DisplayName("GET /api/admin/late-returns - Should filter by date range")
    void shouldFilterByDateRange() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(6);
        LocalDate endDate = LocalDate.now().minusDays(1);

        mockMvc.perform(get("/api/admin/late-returns")
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
    @DisplayName("GET /api/admin/late-returns - Should filter by status")
    void shouldFilterByStatus() throws Exception {
        mockMvc.perform(get("/api/admin/late-returns")
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
    @DisplayName("GET /api/admin/late-returns - Should sort by penalty amount")
    void shouldSortByPenaltyAmount() throws Exception {
        mockMvc.perform(get("/api/admin/late-returns")
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
    @DisplayName("GET /api/admin/late-returns/statistics - Should return statistics")
    void shouldReturnStatistics() throws Exception {
        mockMvc.perform(get("/api/admin/late-returns/statistics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.totalLateReturns").value(2))
                .andExpect(jsonPath("$.severelyLateCount").value(1))
                .andExpect(jsonPath("$.totalPenaltyAmount").value(6500))
                .andExpect(jsonPath("$.collectedPenaltyAmount").value(5000))
                .andExpect(jsonPath("$.pendingPenaltyAmount").value(1500))
                .andExpect(jsonPath("$.averageLateHours").exists())
                .andExpect(jsonPath("$.lateReturnPercentage").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/admin/late-returns/statistics - Should filter statistics by date range")
    void shouldFilterStatisticsByDateRange() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(6);
        LocalDate endDate = LocalDate.now().minusDays(1);

        mockMvc.perform(get("/api/admin/late-returns/statistics")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.totalLateReturns").exists())
                .andExpect(jsonPath("$.totalPenaltyAmount").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/admin/late-returns - Should deny access for non-admin")
    void shouldDenyAccessForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/late-returns"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/late-returns - Should deny access for unauthenticated")
    void shouldDenyAccessForUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/late-returns"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/admin/late-returns/statistics - Should deny access for non-admin")
    void shouldDenyAccessForNonAdminStatistics() throws Exception {
        mockMvc.perform(get("/api/admin/late-returns/statistics"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/late-returns/statistics - Should deny access for unauthenticated")
    void shouldDenyAccessForUnauthenticatedStatistics() throws Exception {
        mockMvc.perform(get("/api/admin/late-returns/statistics"))
                .andExpect(status().isForbidden());
    }
}
