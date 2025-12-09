package com.akif.controller;

import com.akif.dto.damage.request.DamageAssessmentRequestDto;
import com.akif.dto.damage.request.DamageReportRequestDto;
import com.akif.model.Car;
import com.akif.model.Rental;
import com.akif.model.User;
import com.akif.repository.CarRepository;
import com.akif.repository.RentalRepository;
import com.akif.repository.UserRepository;
import com.akif.security.JwtTokenProvider;
import com.akif.shared.enums.*;
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
@DisplayName("DamageAssessmentController Integration Tests")
class DamageAssessmentControllerIntegrationTest {

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
                .username("assessuser")
                .email("assess@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();
        testUser = userRepository.save(testUser);

        adminUser = User.builder()
                .username("assessadmin")
                .email("assessadmin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .roles(Set.of(Role.USER, Role.ADMIN))
                .enabled(true)
                .build();
        adminUser = userRepository.save(adminUser);

        testCar = Car.builder()
                .licensePlate("34ASS001")
                .brand("Mercedes")
                .model("C200")
                .productionYear(2021)
                .price(new BigDecimal("800.00"))
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
                .startDate(LocalDate.now().minusDays(3))
                .endDate(LocalDate.now().plusDays(4))
                .days(7)
                .dailyPrice(new BigDecimal("800.00"))
                .totalPrice(new BigDecimal("5600.00"))
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

    private Long createDamageReport() throws Exception {
        DamageReportRequestDto request = new DamageReportRequestDto(
                "Test damage for assessment",
                "Test location",
                DamageSeverity.MODERATE,
                DamageCategory.DENT
        );

        String response = mockMvc.perform(post("/api/admin/damages")
                        .header("Authorization", adminToken)
                        .param("rentalId", testRental.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    @Nested
    @DisplayName("Assess Damage Tests")
    class AssessDamageTests {

        @Test
        @DisplayName("Should assess damage successfully as admin")
        void shouldAssessDamageSuccessfullyAsAdmin() throws Exception {
            Long damageId = createDamageReport();

            DamageAssessmentRequestDto request = new DamageAssessmentRequestDto(
                    DamageSeverity.MODERATE,
                    DamageCategory.DENT,
                    new BigDecimal("2500.00"),
                    true,
                    new BigDecimal("250.00"),
                    "Moderate dent requires professional repair"
            );

            mockMvc.perform(post("/api/admin/damages/{id}/assess", damageId)
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.damageId").value(damageId))
                    .andExpect(jsonPath("$.repairCostEstimate").value(2500.00));
        }

        @Test
        @DisplayName("Should return 403 when user tries to assess damage")
        void shouldReturn403WhenUserTriesToAssessDamage() throws Exception {
            Long damageId = createDamageReport();

            DamageAssessmentRequestDto request = new DamageAssessmentRequestDto(
                    DamageSeverity.MINOR,
                    DamageCategory.SCRATCH,
                    new BigDecimal("1000.00"),
                    false,
                    null,
                    "Test notes"
            );

            mockMvc.perform(post("/api/admin/damages/{id}/assess", damageId)
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should calculate liability without insurance")
        void shouldCalculateLiabilityWithoutInsurance() throws Exception {
            Long damageId = createDamageReport();

            DamageAssessmentRequestDto request = new DamageAssessmentRequestDto(
                    DamageSeverity.MODERATE,
                    DamageCategory.DENT,
                    new BigDecimal("1500.00"),
                    false,
                    null,
                    "No insurance coverage"
            );

            mockMvc.perform(post("/api/admin/damages/{id}/assess", damageId)
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.repairCostEstimate").value(1500.00));
        }

        @Test
        @DisplayName("Should return 404 when damage report not found")
        void shouldReturn404WhenDamageReportNotFound() throws Exception {
            DamageAssessmentRequestDto request = new DamageAssessmentRequestDto(
                    DamageSeverity.MINOR,
                    DamageCategory.SCRATCH,
                    new BigDecimal("1000.00"),
                    false,
                    null,
                    "Test"
            );

            mockMvc.perform(post("/api/admin/damages/{id}/assess", 99999)
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Update Assessment Tests")
    class UpdateAssessmentTests {

        @Test
        @DisplayName("Should update assessment when status allows")
        void shouldUpdateAssessmentWhenStatusAllows() throws Exception {
            Long damageId = createDamageReport();

            DamageAssessmentRequestDto initialRequest = new DamageAssessmentRequestDto(
                    DamageSeverity.MODERATE,
                    DamageCategory.DENT,
                    new BigDecimal("1000.00"),
                    false,
                    null,
                    "Initial assessment"
            );

            mockMvc.perform(post("/api/admin/damages/{id}/assess", damageId)
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(initialRequest)))
                    .andExpect(status().isOk());

            DamageAssessmentRequestDto updateRequest = new DamageAssessmentRequestDto(
                    DamageSeverity.MAJOR,
                    DamageCategory.DENT,
                    new BigDecimal("1500.00"),
                    true,
                    new BigDecimal("150.00"),
                    "Updated assessment with insurance"
            );

            mockMvc.perform(put("/api/admin/damages/{id}/assess", damageId)
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.repairCostEstimate").value(1500.00));
        }
    }
}
