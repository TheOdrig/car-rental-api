package com.akif.damage.integration;

import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.car.internal.repository.CarRepository;
import com.akif.damage.domain.enums.DamageCategory;
import com.akif.damage.domain.enums.DamageSeverity;
import com.akif.damage.internal.dto.damage.request.DamageReportRequest;
import com.akif.rental.domain.enums.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.repository.RentalRepository;
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
@DisplayName("DamageHistoryController Integration Tests")
class DamageHistoryControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

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

    private MockMvc mockMvc;

    private User testUser;
    private User adminUser;
    private Car testCar;
    private Rental testRental;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        testUser = User.builder()
                .username("historyuser")
                .email("history@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();
        testUser = userRepository.save(testUser);

        adminUser = User.builder()
                .username("historyadmin")
                .email("historyadmin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .roles(Set.of(Role.USER, Role.ADMIN))
                .enabled(true)
                .build();
        adminUser = userRepository.save(adminUser);

        testCar = Car.builder()
                .licensePlate("34HIS001")
                .brand("Volkswagen")
                .model("Passat")
                .productionYear(2022)
                .price(new BigDecimal("750.00"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .isFeatured(false)
                .isTestDriveAvailable(true)
                .viewCount(0L)
                .likeCount(0L)
                .build();
        testCar = carRepository.save(testCar);

        testRental = Rental.builder()
                .userId(testUser.getId())
                .userEmail(testUser.getEmail())
                .userFullName(testUser.getUsername())
                .carId(testCar.getId())
                .carBrand(testCar.getBrand())
                .carModel(testCar.getModel())
                .carLicensePlate(testCar.getLicensePlate())
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(3))
                .days(7)
                .dailyPrice(new BigDecimal("750.00"))
                .totalPrice(new BigDecimal("5250.00"))
                .currency(CurrencyType.TRY)
                .status(RentalStatus.RETURNED)
                .hasDamageReports(false)
                .damageReportsCount(0)
                .build();
        testRental = rentalRepository.save(testRental);

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

    private void createDamageReports() throws Exception {
        DamageReportRequest request1 = new DamageReportRequest(
                "Scratch on hood",
                "Hood",
                DamageSeverity.MINOR,
                DamageCategory.SCRATCH
        );

        mockMvc.perform(post("/api/admin/damages")
                        .header("Authorization", adminToken)
                        .param("rentalId", testRental.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        DamageReportRequest request2 = new DamageReportRequest(
                "Dent on door",
                "Driver side door",
                DamageSeverity.MODERATE,
                DamageCategory.DENT
        );

        mockMvc.perform(post("/api/admin/damages")
                        .header("Authorization", adminToken)
                        .param("rentalId", testRental.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());
    }

    @Nested
    @DisplayName("Get Vehicle Damage History Tests")
    class GetVehicleDamageHistoryTests {

        @Test
        @DisplayName("Should get vehicle damage history as admin")
        void shouldGetVehicleDamageHistoryAsAdmin() throws Exception {
            createDamageReports();

            mockMvc.perform(get("/api/admin/damages/vehicle/{carId}", testCar.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].carLicensePlate").value("34HIS001"));
        }

        @Test
        @DisplayName("Should return 403 when user tries to access vehicle damage history")
        void shouldReturn403WhenUserTriesToAccessVehicleDamageHistory() throws Exception {
            mockMvc.perform(get("/api/admin/damages/vehicle/{carId}", testCar.getId())
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return empty page for vehicle with no damages")
        void shouldReturnEmptyPageForVehicleWithNoDamages() throws Exception {
            mockMvc.perform(get("/api/admin/damages/vehicle/{carId}", testCar.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }
    }

    @Nested
    @DisplayName("Get Customer Damage History Tests")
    class GetCustomerDamageHistoryTests {

        @Test
        @DisplayName("Should get customer damage history as admin")
        void shouldGetCustomerDamageHistoryAsAdmin() throws Exception {
            createDamageReports();

            mockMvc.perform(get("/api/admin/damages/customer/{userId}", testUser.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2));
        }

        @Test
        @DisplayName("Should return 403 when user tries to access other customer's damage history")
        void shouldReturn403WhenUserTriesToAccessOtherCustomersDamageHistory() throws Exception {
            mockMvc.perform(get("/api/admin/damages/customer/{userId}", testUser.getId())
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Get My Damage History Tests")
    class GetMyDamageHistoryTests {

        @Test
        @DisplayName("Should get own damage history as user")
        void shouldGetOwnDamageHistoryAsUser() throws Exception {
            createDamageReports();

            mockMvc.perform(get("/api/damages/me")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2));
        }

        @Test
        @DisplayName("Should return 403 when unauthenticated")
        void shouldReturn403WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/damages/me"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Search Damages Tests")
    class SearchDamagesTests {

        @Test
        @DisplayName("Should search damages by severity as admin")
        void shouldSearchDamagesBySeverityAsAdmin() throws Exception {
            createDamageReports();

            mockMvc.perform(get("/api/admin/damages/search")
                            .header("Authorization", adminToken)
                            .param("severity", "MINOR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].severity").value("MINOR"));
        }

        @Test
        @DisplayName("Should search damages by category as admin")
        void shouldSearchDamagesByCategoryAsAdmin() throws Exception {
            createDamageReports();

            mockMvc.perform(get("/api/admin/damages/search")
                            .header("Authorization", adminToken)
                            .param("category", "SCRATCH"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].category").value("SCRATCH"));
        }

        @Test
        @DisplayName("Should search damages by car ID as admin")
        void shouldSearchDamagesByCarIdAsAdmin() throws Exception {
            createDamageReports();

            mockMvc.perform(get("/api/admin/damages/search")
                            .header("Authorization", adminToken)
                            .param("carId", testCar.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2));
        }

        @Test
        @DisplayName("Should return 403 when user tries to search damages")
        void shouldReturn403WhenUserTriesToSearchDamages() throws Exception {
            mockMvc.perform(get("/api/admin/damages/search")
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Get Damage Statistics Tests")
    class GetDamageStatisticsTests {

        @Test
        @DisplayName("Should get damage statistics as admin")
        void shouldGetDamageStatisticsAsAdmin() throws Exception {
            createDamageReports();

            mockMvc.perform(get("/api/admin/damages/statistics")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDamages").exists())
                    .andExpect(jsonPath("$.totalRepairCost").exists());
        }

        @Test
        @DisplayName("Should get damage statistics with date range")
        void shouldGetDamageStatisticsWithDateRange() throws Exception {
            createDamageReports();

            mockMvc.perform(get("/api/admin/damages/statistics")
                            .header("Authorization", adminToken)
                            .param("startDate", LocalDate.now().minusDays(30).toString())
                            .param("endDate", LocalDate.now().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDamages").exists());
        }

        @Test
        @DisplayName("Should return 403 when user tries to access statistics")
        void shouldReturn403WhenUserTriesToAccessStatistics() throws Exception {
            mockMvc.perform(get("/api/admin/damages/statistics")
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }
    }
}
