package com.akif.controller;

import com.akif.dto.damage.request.DamageAssessmentRequestDto;
import com.akif.dto.damage.request.DamageDisputeRequestDto;
import com.akif.dto.damage.request.DamageDisputeResolutionDto;
import com.akif.dto.damage.request.DamageReportRequestDto;
import com.akif.enums.*;
import com.akif.model.Car;
import com.akif.model.Rental;
import com.akif.model.User;
import com.akif.repository.CarRepository;
import com.akif.repository.DamageReportRepository;
import com.akif.repository.RentalRepository;
import com.akif.repository.UserRepository;
import jakarta.persistence.EntityManager;
import com.akif.security.JwtTokenProvider;
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
@DisplayName("DamageDisputeController Integration Tests")
class DamageDisputeControllerIntegrationTest {

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
    private DamageReportRepository damageReportRepository;

    @Autowired
    private EntityManager entityManager;

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
                .username("disputeuser")
                .email("dispute@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();
        testUser = userRepository.save(testUser);

        adminUser = User.builder()
                .username("disputeadmin")
                .email("disputeadmin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .roles(Set.of(Role.USER, Role.ADMIN))
                .enabled(true)
                .build();
        adminUser = userRepository.save(adminUser);

        testCar = Car.builder()
                .licensePlate("34DIS001")
                .brand("Audi")
                .model("A4")
                .productionYear(2023)
                .price(new BigDecimal("900.00"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .isFeatured(false)
                .isTestDriveAvailable(true)
                .viewCount(0L)
                .likeCount(0L)
                .build();
        testCar = carRepository.save(testCar);

        testRental = Rental.builder()
                .user(testUser)
                .car(testCar)
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(2))
                .days(7)
                .dailyPrice(new BigDecimal("900.00"))
                .totalPrice(new BigDecimal("6300.00"))
                .currency(CurrencyType.TRY)
                .status(RentalStatus.IN_USE)
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

    private Long createAndAssessDamageReport() throws Exception {
        DamageReportRequestDto reportRequest = new DamageReportRequestDto(
                "Disputed damage",
                "Side panel",
                DamageSeverity.MODERATE,
                DamageCategory.DENT
        );

        String reportResponse = mockMvc.perform(post("/api/admin/damages")
                        .header("Authorization", adminToken)
                        .param("rentalId", testRental.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long damageId = objectMapper.readTree(reportResponse).get("id").asLong();

        DamageAssessmentRequestDto assessRequest = new DamageAssessmentRequestDto(
                DamageSeverity.MODERATE,
                DamageCategory.DENT,
                new BigDecimal("2000.00"),
                false,
                null,
                "Assessment for dispute test"
        );

        mockMvc.perform(post("/api/admin/damages/{id}/assess", damageId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assessRequest)))
                .andExpect(status().isOk());

        return damageId;
    }

    private void chargeDamageReport(Long damageId) {
        var report = damageReportRepository.findById(damageId).orElseThrow();
        report.updateStatus(DamageStatus.CHARGED);
        damageReportRepository.save(report);
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("Create Dispute Tests")
    class CreateDisputeTests {

        @Test
        @DisplayName("Should create dispute successfully as rental owner")
        void shouldCreateDisputeSuccessfullyAsRentalOwner() throws Exception {
            Long damageId = createAndAssessDamageReport();
            chargeDamageReport(damageId);

            DamageDisputeRequestDto request = new DamageDisputeRequestDto(
                    "I disagree with this damage assessment",
                    "The damage was pre-existing when I picked up the car"
            );

            mockMvc.perform(post("/api/damages/{id}/dispute", damageId)
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.damageId").value(damageId))
                    .andExpect(jsonPath("$.status").value("DISPUTED"))
                    .andExpect(jsonPath("$.disputeReason").value("I disagree with this damage assessment"));
        }

        @Test
        @DisplayName("Should return 403 when unauthenticated user tries to dispute")
        void shouldReturn403WhenUnauthenticatedUserTriesToDispute() throws Exception {
            Long damageId = createAndAssessDamageReport();
            chargeDamageReport(damageId);

            DamageDisputeRequestDto request = new DamageDisputeRequestDto(
                    "Dispute reason",
                    "Comments"
            );

            mockMvc.perform(post("/api/damages/{id}/dispute", damageId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 when damage report not found")
        void shouldReturn404WhenDamageReportNotFound() throws Exception {
            DamageDisputeRequestDto request = new DamageDisputeRequestDto(
                    "Dispute reason",
                    "Comments"
            );

            mockMvc.perform(post("/api/damages/{id}/dispute", 99999)
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Resolve Dispute Tests")
    class ResolveDisputeTests {

        @Test
        @DisplayName("Should resolve dispute as admin with adjusted liability")
        void shouldResolveDisputeAsAdminWithAdjustedLiability() throws Exception {
            Long damageId = createAndAssessDamageReport();
            chargeDamageReport(damageId);

            DamageDisputeRequestDto disputeRequest = new DamageDisputeRequestDto(
                    "Amount is too high",
                    "I want reconsideration"
            );

            mockMvc.perform(post("/api/damages/{id}/dispute", damageId)
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(disputeRequest)))
                    .andExpect(status().isCreated());

            DamageDisputeResolutionDto resolution = new DamageDisputeResolutionDto(
                    new BigDecimal("1500.00"),
                    new BigDecimal("1000.00"),
                    "After review, liability adjusted to 50%"
            );

            mockMvc.perform(post("/api/admin/damages/{id}/resolve", damageId)
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resolution)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RESOLVED"))
                    .andExpect(jsonPath("$.adjustedLiability").value(1000.00))
                    .andExpect(jsonPath("$.resolutionNotes").value("After review, liability adjusted to 50%"));
        }

        @Test
        @DisplayName("Should reject dispute as admin")
        void shouldRejectDisputeAsAdmin() throws Exception {
            Long damageId = createAndAssessDamageReport();
            chargeDamageReport(damageId);

            DamageDisputeRequestDto disputeRequest = new DamageDisputeRequestDto(
                    "Invalid dispute reason",
                    "No valid evidence"
            );

            mockMvc.perform(post("/api/damages/{id}/dispute", damageId)
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(disputeRequest)))
                    .andExpect(status().isCreated());

            DamageDisputeResolutionDto resolution = new DamageDisputeResolutionDto(
                    new BigDecimal("2000.00"),
                    new BigDecimal("2000.00"),
                    "Dispute rejected - damage clearly occurred during rental period"
            );

            mockMvc.perform(post("/api/admin/damages/{id}/resolve", damageId)
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resolution)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RESOLVED"));
        }

        @Test
        @DisplayName("Should return 403 when user tries to resolve dispute")
        void shouldReturn403WhenUserTriesToResolveDispute() throws Exception {
            Long damageId = createAndAssessDamageReport();
            chargeDamageReport(damageId);

            DamageDisputeRequestDto disputeRequest = new DamageDisputeRequestDto(
                    "Dispute reason",
                    "Comments"
            );

            mockMvc.perform(post("/api/damages/{id}/dispute", damageId)
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(disputeRequest)))
                    .andExpect(status().isCreated());

            DamageDisputeResolutionDto resolution = new DamageDisputeResolutionDto(
                    new BigDecimal("1000.00"),
                    new BigDecimal("500.00"),
                    "User trying to resolve"
            );

            mockMvc.perform(post("/api/admin/damages/{id}/resolve", damageId)
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resolution)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Complete Dispute Workflow Tests")
    class CompleteDisputeWorkflowTests {

        @Test
        @DisplayName("Should complete full dispute workflow: report -> assess -> dispute -> resolve")
        void shouldCompleteFullDisputeWorkflow() throws Exception {
            DamageReportRequestDto reportRequest = new DamageReportRequestDto(
                    "Tire damage found",
                    "Rear left tire",
                    DamageSeverity.MINOR,
                    DamageCategory.TIRE_DAMAGE
            );

            String reportResponse = mockMvc.perform(post("/api/admin/damages")
                            .header("Authorization", adminToken)
                            .param("rentalId", testRental.getId().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reportRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("REPORTED"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long damageId = objectMapper.readTree(reportResponse).get("id").asLong();

            DamageAssessmentRequestDto assessRequest = new DamageAssessmentRequestDto(
                    DamageSeverity.MINOR,
                    DamageCategory.TIRE_DAMAGE,
                    new BigDecimal("800.00"),
                    false,
                    null,
                    "Tire replacement needed"
            );

            mockMvc.perform(post("/api/admin/damages/{id}/assess", damageId)
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(assessRequest)))
                    .andExpect(status().isOk());

            chargeDamageReport(damageId);

            DamageDisputeRequestDto disputeRequest = new DamageDisputeRequestDto(
                    "Tire was already worn when I picked up",
                    "I have photos from pickup showing worn tire"
            );

            mockMvc.perform(post("/api/damages/{id}/dispute", damageId)
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(disputeRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("DISPUTED"));

            DamageDisputeResolutionDto resolution = new DamageDisputeResolutionDto(
                    new BigDecimal("800.00"),
                    new BigDecimal("400.00"),
                    "50% reduction due to pre-existing wear"
            );

            mockMvc.perform(post("/api/admin/damages/{id}/resolve", damageId)
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resolution)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RESOLVED"))
                    .andExpect(jsonPath("$.adjustedLiability").value(400.00));
        }
    }
}
