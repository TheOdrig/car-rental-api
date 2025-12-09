package com.akif.controller;

import com.akif.dto.damage.request.DamageReportRequestDto;
import com.akif.model.Car;
import com.akif.model.Rental;
import com.akif.model.User;
import com.akif.repository.CarRepository;
import com.akif.repository.RentalRepository;
import com.akif.repository.UserRepository;
import com.akif.shared.security.JwtTokenProvider;
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
import org.springframework.mock.web.MockMultipartFile;
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
@DisplayName("DamageReportController Integration Tests")
class DamageReportControllerIntegrationTest {

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
                .username("damageuser")
                .email("damage@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();
        testUser = userRepository.save(testUser);

        adminUser = User.builder()
                .username("damageadmin")
                .email("damageadmin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .roles(Set.of(Role.USER, Role.ADMIN))
                .enabled(true)
                .build();
        adminUser = userRepository.save(adminUser);

        testCar = Car.builder()
                .licensePlate("34DMG001")
                .brand("BMW")
                .model("X5")
                .productionYear(2022)
                .price(new BigDecimal("1000.00"))
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
                .dailyPrice(new BigDecimal("1000.00"))
                .totalPrice(new BigDecimal("7000.00"))
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

    @Nested
    @DisplayName("Create Damage Report Tests")
    class CreateDamageReportTests {

        @Test
        @DisplayName("Should create damage report successfully as admin")
        void shouldCreateDamageReportSuccessfullyAsAdmin() throws Exception {
            DamageReportRequestDto request = new DamageReportRequestDto(
                    "Scratch on front bumper",
                    "Front bumper, left side",
                    DamageSeverity.MINOR,
                    DamageCategory.SCRATCH
            );

            mockMvc.perform(post("/api/admin/damages")
                            .header("Authorization", adminToken)
                            .param("rentalId", testRental.getId().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.description").value("Scratch on front bumper"))
                    .andExpect(jsonPath("$.status").value("REPORTED"));
        }

        @Test
        @DisplayName("Should return 403 when user tries to create damage report")
        void shouldReturn403WhenUserTriesToCreateDamageReport() throws Exception {
            DamageReportRequestDto request = new DamageReportRequestDto(
                    "Test damage",
                    "Test location",
                    DamageSeverity.MINOR,
                    DamageCategory.SCRATCH
            );

            mockMvc.perform(post("/api/admin/damages")
                            .header("Authorization", userToken)
                            .param("rentalId", testRental.getId().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when unauthenticated")
        void shouldReturn403WhenUnauthenticated() throws Exception {
            DamageReportRequestDto request = new DamageReportRequestDto(
                    "Test damage",
                    "Test location",
                    DamageSeverity.MINOR,
                    DamageCategory.SCRATCH
            );

            mockMvc.perform(post("/api/admin/damages")
                            .param("rentalId", testRental.getId().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 when rental not found")
        void shouldReturn404WhenRentalNotFound() throws Exception {
            DamageReportRequestDto request = new DamageReportRequestDto(
                    "Test damage",
                    "Test location",
                    DamageSeverity.MINOR,
                    DamageCategory.SCRATCH
            );

            mockMvc.perform(post("/api/admin/damages")
                            .header("Authorization", adminToken)
                            .param("rentalId", "99999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Get Damage Report Tests")
    class GetDamageReportTests {

        @Test
        @DisplayName("Should get damage report by ID as admin")
        void shouldGetDamageReportByIdAsAdmin() throws Exception {
            DamageReportRequestDto request = new DamageReportRequestDto(
                    "Glass damage on windshield",
                    "Windshield",
                    DamageSeverity.MODERATE,
                    DamageCategory.GLASS_DAMAGE
            );

            String createResponse = mockMvc.perform(post("/api/admin/damages")
                            .header("Authorization", adminToken)
                            .param("rentalId", testRental.getId().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long damageId = objectMapper.readTree(createResponse).get("id").asLong();

            mockMvc.perform(get("/api/admin/damages/{id}", damageId)
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(damageId))
                    .andExpect(jsonPath("$.description").value("Glass damage on windshield"))
                    .andExpect(jsonPath("$.category").value("GLASS_DAMAGE"));
        }

        @Test
        @DisplayName("Should return 404 when damage report not found")
        void shouldReturn404WhenDamageReportNotFound() throws Exception {
            mockMvc.perform(get("/api/admin/damages/{id}", 99999)
                            .header("Authorization", adminToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Upload Photos Tests")
    class UploadPhotosTests {

        @Test
        @DisplayName("Should upload photos successfully")
        void shouldUploadPhotosSuccessfully() throws Exception {
            DamageReportRequestDto request = new DamageReportRequestDto(
                    "Dent on door",
                    "Driver side door",
                    DamageSeverity.MINOR,
                    DamageCategory.DENT
            );

            String createResponse = mockMvc.perform(post("/api/admin/damages")
                            .header("Authorization", adminToken)
                            .param("rentalId", testRental.getId().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long damageId = objectMapper.readTree(createResponse).get("id").asLong();

            MockMultipartFile photo = new MockMultipartFile(
                    "photos",
                    "damage-photo.jpg",
                    "image/jpeg",
                    "test image content".getBytes()
            );

            mockMvc.perform(multipart("/api/admin/damages/{id}/photos", damageId)
                            .file(photo)
                            .header("Authorization", adminToken))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[0].id").exists())
                    .andExpect(jsonPath("$[0].fileName").value("damage-photo.jpg"));
        }
    }

    @Nested
    @DisplayName("Complete Damage Workflow Tests")
    class CompleteDamageWorkflowTests {

        @Test
        @DisplayName("Should complete full damage workflow: report -> assess -> resolved")
        void shouldCompleteFullDamageWorkflow() throws Exception {
            DamageReportRequestDto reportRequest = new DamageReportRequestDto(
                    "Major mechanical damage",
                    "Engine compartment",
                    DamageSeverity.MAJOR,
                    DamageCategory.MECHANICAL_DAMAGE
            );

            String createResponse = mockMvc.perform(post("/api/admin/damages")
                            .header("Authorization", adminToken)
                            .param("rentalId", testRental.getId().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reportRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("REPORTED"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long damageId = objectMapper.readTree(createResponse).get("id").asLong();

            String assessmentRequest = """
                {
                    "severity": "MAJOR",
                    "category": "MECHANICAL_DAMAGE",
                    "repairCostEstimate": 5000.00,
                    "insuranceCoverage": true,
                    "insuranceDeductible": 500.00,
                    "assessmentNotes": "Engine damage requires replacement"
                }
                """;

            mockMvc.perform(post("/api/admin/damages/{id}/assess", damageId)
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(assessmentRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.damageId").value(damageId))
                    .andExpect(jsonPath("$.repairCostEstimate").value(5000.00));
        }
    }
}
