package com.akif.damage.e2e;

import java.time.Duration;
import com.akif.CarRentalApplication;
import org.springframework.test.util.ReflectionTestUtils;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.api.CarStatusType;
import com.akif.car.internal.repository.CarRepository;
import com.akif.damage.api.DamageAssessedEvent;
import com.akif.damage.api.DamageChargedEvent;
import com.akif.damage.api.DamageDisputedEvent;
import com.akif.damage.api.DamageReportedEvent;
import com.akif.damage.api.DamageResolvedEvent;
import com.akif.damage.api.DamageCategory;
import com.akif.damage.api.DamageSeverity;
import com.akif.damage.api.DamageStatus;
import com.akif.damage.domain.model.DamageReport;
import com.akif.damage.internal.dto.damage.request.DamageAssessmentRequest;
import com.akif.damage.internal.dto.damage.request.DamageDisputeRequest;
import com.akif.damage.internal.dto.damage.request.DamageDisputeResolutionDto;
import com.akif.damage.internal.dto.damage.request.DamageReportRequest;
import com.akif.damage.internal.repository.DamageReportRepository;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.api.RentalRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.akif.testsupport.TestcontainersConfiguration;
import org.springframework.http.MediaType;
import com.akif.testsupport.ImageFixtures;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@DisplayName("Damage Management E2E Tests")
class DamageManagementE2ETest extends E2ETestBase {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private CarRepository carRepository;

        @Autowired
        private DamageReportRepository damageReportRepository;

        private BigDecimal dashboardDamageRecovered(String adminToken, Long damageId) throws Exception {
                String currency = damageReportRepository.findById(damageId).orElseThrow().getCurrency().name();
                String body = mockMvc.perform(get("/api/v1/admin/dashboard/revenue")
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                for (var row : objectMapper.readTree(body).get("breakdownByCurrency")) {
                        if (currency.equals(row.get("currency").asString())) {
                                return row.get("damageRecovered").decimalValue();
                        }
                }
                return BigDecimal.ZERO;
        }

        @Nested
        @DisplayName("Complete Damage Flow E2E Tests")
        class CompleteDamageFlowE2ETests {

                @Test
                @DisplayName("Should complete full damage lifecycle: rental → return → damage report → assess → charge → dispute → resolve")
                void shouldCompleteFullDamageLifecycle() throws Exception {
                        User customer = TestDataBuilder.createTestUser();
                        customer = userRepository.save(customer);
                        String customerToken = generateUserToken(customer);

                        User admin = TestDataBuilder.createTestAdmin();
                        admin = userRepository.save(admin);
                        String adminToken = generateAdminToken(admin);

                        Car car = TestDataBuilder.createAvailableCar();
                        car = carRepository.save(car);

                        RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(car.getId());
                        Long rentalId = createAndGetRentalId(rentalRequest, customerToken);

                        mockMvc.perform(post("/api/v1/rentals/{id}/confirm", rentalId)
                                        .header("Authorization", "Bearer " + adminToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.status").value(RentalStatus.CONFIRMED.name()));

                        payRental(rentalId);
                        moveStartToToday(rentalId);
                        mockMvc.perform(post("/api/v1/rentals/{id}/pickup", rentalId)
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{}"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.status").value(RentalStatus.IN_USE.name()));

                        mockMvc.perform(post("/api/v1/rentals/{id}/return", rentalId)
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{}"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.status").value(RentalStatus.RETURNED.name()));

                        eventCaptor.clear();

                        DamageReportRequest damageRequest = new DamageReportRequest(
                                        "Significant dent found on driver side door during return inspection",
                                        "Driver side door, lower panel",
                                        DamageSeverity.MODERATE,
                                        DamageCategory.DENT);

                        String damageResponse = mockMvc.perform(post("/api/v1/admin/damages")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .param("rentalId", rentalId.toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(damageRequest)))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.id").exists())
                                        .andExpect(jsonPath("$.status").value("REPORTED"))
                                        .andExpect(jsonPath("$.rentalId").value(rentalId))
                                        .andExpect(jsonPath("$.description").value(
                                                        "Significant dent found on driver side door during return inspection"))
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString();

                        Long damageId = objectMapper.readTree(damageResponse).get("id").asLong();

                        List<DamageReportedEvent> reportedEvents = eventCaptor
                                        .getEventsOfType(DamageReportedEvent.class);
                        assertThat(reportedEvents).hasSize(1);
                        assertThat(reportedEvents.get(0).damageReportId()).isEqualTo(damageId);

                        MockMultipartFile photo1 = new MockMultipartFile(
                                        "photos",
                                        "damage-photo-1.jpg",
                                        "image/jpeg",
                                        ImageFixtures.jpeg());

                        MockMultipartFile photo2 = new MockMultipartFile(
                                        "photos",
                                        "damage-photo-2.jpg",
                                        "image/jpeg",
                                        ImageFixtures.jpeg());

                        mockMvc.perform(multipart("/api/v1/admin/damages/{id}/photos", damageId)
                                        .file(photo1)
                                        .file(photo2)
                                        .header("Authorization", "Bearer " + adminToken))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$").isArray())
                                        .andExpect(jsonPath("$.length()").value(2));

                        mockMvc.perform(get("/api/v1/admin/damages/{id}", damageId)
                                        .header("Authorization", "Bearer " + adminToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.photos").exists())
                                        .andExpect(jsonPath("$.photos.length()").value(2));

                        mockMvc.perform(get("/api/v1/damages/{id}", damageId)
                                        .header("Authorization", "Bearer " + customerToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.photos.length()").value(2))
                                        .andExpect(jsonPath("$.photos[0].secureUrl").value(
                                                        org.hamcrest.Matchers.startsWith("/api/v1/files/damage-photos/")));

                        mockMvc.perform(get("/api/v1/damages/me")
                                        .header("Authorization", "Bearer " + customerToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.content[0].photos.length()").value(2));

                        User stranger = userRepository.save(TestDataBuilder.createTestUser());
                        mockMvc.perform(get("/api/v1/damages/{id}", damageId)
                                        .header("Authorization", "Bearer " + generateUserToken(stranger)))
                                        .andExpect(status().isNotFound());

                        eventCaptor.clear();

                        DamageAssessmentRequest assessmentRequest = new DamageAssessmentRequest(
                                        DamageSeverity.MODERATE,
                                        DamageCategory.DENT,
                                        new BigDecimal("1500.00"),
                                        false,
                                        null,
                                        "Door panel needs replacement. Labor and parts included in estimate.");

                        mockMvc.perform(post("/api/v1/admin/damages/{id}/assess", damageId)
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(assessmentRequest)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.damageId").value(damageId))
                                        .andExpect(jsonPath("$.severity").value("MODERATE"))
                                        .andExpect(jsonPath("$.repairCostEstimate").value(1500.00))
                                        .andExpect(jsonPath("$.customerLiability").value(1500.00));

                        List<DamageAssessedEvent> assessedEvents = eventCaptor
                                        .getEventsOfType(DamageAssessedEvent.class);
                        assertThat(assessedEvents).hasSize(1);
                        assertThat(assessedEvents.get(0).damageReportId()).isEqualTo(damageId);

                        DamageReport damageReport = damageReportRepository.findById(damageId).orElseThrow();
                        assertThat(damageReport.getStatus()).isEqualTo(DamageStatus.ASSESSED);

                        eventCaptor.clear();

                        mockMvc.perform(post("/api/v1/admin/damages/{id}/charge", damageId)
                                        .header("Authorization", "Bearer " + adminToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.damageId").value(damageId))
                                        .andExpect(jsonPath("$.charged").value(true))
                                        .andExpect(jsonPath("$.status").value("CHARGED"))
                                        .andExpect(jsonPath("$.amount").value(1500.00))
                                        .andExpect(jsonPath("$.transactionId").isNotEmpty());

                        assertThat(damageReportRepository.findById(damageId).orElseThrow().getStatus())
                                        .isEqualTo(DamageStatus.CHARGED);
                        assertThat(eventCaptor.getEventsOfType(DamageChargedEvent.class)).hasSize(1);
                        assertThat(jdbcTemplate.queryForObject(
                                        "SELECT status FROM car_rental.payments WHERE kind = 'DAMAGE' AND reference_id = ?",
                                        String.class, damageId)).isEqualTo("CAPTURED");

                        mockMvc.perform(post("/api/v1/admin/damages/{id}/charge", damageId)
                                        .header("Authorization", "Bearer " + adminToken))
                                        .andExpect(status().isBadRequest());

                        BigDecimal recoveredAfterCharge = dashboardDamageRecovered(adminToken, damageId);

                        eventCaptor.clear();

                        DamageDisputeRequest disputeRequest = new DamageDisputeRequest(
                                        "I disagree with this damage assessment",
                                        "The dent was pre-existing when I picked up the car. I have photos from pickup.");

                        mockMvc.perform(post("/api/v1/damages/{id}/dispute", damageId)
                                        .header("Authorization", "Bearer " + customerToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(disputeRequest)))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.damageId").value(damageId))
                                        .andExpect(jsonPath("$.status").value("DISPUTED"))
                                        .andExpect(jsonPath("$.disputeReason")
                                                        .value("I disagree with this damage assessment"));

                        List<DamageDisputedEvent> disputedEvents = eventCaptor
                                        .getEventsOfType(DamageDisputedEvent.class);
                        assertThat(disputedEvents).hasSize(1);
                        assertThat(disputedEvents.get(0).damageReportId()).isEqualTo(damageId);

                        eventCaptor.clear();

                        DamageDisputeResolutionDto resolutionRequest = new DamageDisputeResolutionDto(
                                        new BigDecimal("1500.00"),
                                        new BigDecimal("750.00"),
                                        "After reviewing customer's pickup photos, determined dent was partially visible. Customer liability reduced by 50%.");

                        mockMvc.perform(post("/api/v1/admin/damages/{id}/resolve", damageId)
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(resolutionRequest)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.damageId").value(damageId))
                                        .andExpect(jsonPath("$.status").value("RESOLVED"))
                                        .andExpect(jsonPath("$.adjustedLiability").value(750.00))
                                        .andExpect(jsonPath("$.resolutionNotes").isNotEmpty());

                        List<DamageResolvedEvent> resolvedEvents = eventCaptor
                                        .getEventsOfType(DamageResolvedEvent.class);
                        assertThat(resolvedEvents).hasSize(1);
                        assertThat(resolvedEvents.get(0).damageReportId()).isEqualTo(damageId);
                        assertThat(resolvedEvents.get(0).refundAmount())
                                        .isEqualByComparingTo(new BigDecimal("750.00"));

                        mockMvc.perform(get("/api/v1/admin/damages/{id}", damageId)
                                        .header("Authorization", "Bearer " + adminToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.status").value("RESOLVED"))
                                        .andExpect(jsonPath("$.customerLiability").value(750.00));

                        var recovered = paymentService.sumCollectedBetween(
                                        java.time.Instant.now(clock).minus(Duration.ofDays(1)),
                                        java.time.Instant.now(clock).plus(Duration.ofDays(1)));
                        assertThat(recovered).anySatisfy(row -> {
                                assertThat(row.category()).isEqualTo(com.akif.payment.api.PaymentCategory.DAMAGE);
                                assertThat(row.amount()).isEqualByComparingTo("750.00");
                        });

                        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                                        assertThat(dashboardDamageRecovered(adminToken, damageId))
                                                        .isEqualByComparingTo(recoveredAfterCharge.subtract(new BigDecimal("750.00"))));
                }

                @Test
                @DisplayName("Should update car status to MAINTENANCE when MAJOR damage is assessed")
                void shouldUpdateCarStatusToMaintenanceForMajorDamage() throws Exception {
                        User customer = TestDataBuilder.createTestUser();
                        customer = userRepository.save(customer);
                        String customerToken = generateUserToken(customer);

                        User admin = TestDataBuilder.createTestAdmin();
                        admin = userRepository.save(admin);
                        String adminToken = generateAdminToken(admin);

                        Car car = TestDataBuilder.createAvailableCar();
                        car = carRepository.save(car);

                        RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(car.getId());
                        Long rentalId = createAndGetRentalId(rentalRequest, customerToken);

                        mockMvc.perform(post("/api/v1/rentals/{id}/confirm", rentalId)
                                        .header("Authorization", "Bearer " + adminToken))
                                        .andExpect(status().isOk());

                        payRental(rentalId);
                        moveStartToToday(rentalId);
                        mockMvc.perform(post("/api/v1/rentals/{id}/pickup", rentalId)
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{}"))
                                        .andExpect(status().isOk());

                        mockMvc.perform(post("/api/v1/rentals/{id}/return", rentalId)
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{}"))
                                        .andExpect(status().isOk());

                        DamageReportRequest damageRequest = new DamageReportRequest(
                                        "Severe engine damage - car undrivable",
                                        "Engine compartment",
                                        DamageSeverity.MAJOR,
                                        DamageCategory.MECHANICAL_DAMAGE);

                        String damageResponse = mockMvc.perform(post("/api/v1/admin/damages")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .param("rentalId", rentalId.toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(damageRequest)))
                                        .andExpect(status().isCreated())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString();

                        Long damageId = objectMapper.readTree(damageResponse).get("id").asLong();

                        DamageAssessmentRequest assessmentRequest = new DamageAssessmentRequest(
                                        DamageSeverity.MAJOR,
                                        DamageCategory.MECHANICAL_DAMAGE,
                                        new BigDecimal("8000.00"),
                                        false,
                                        null,
                                        "Engine requires complete overhaul");

                        mockMvc.perform(post("/api/v1/admin/damages/{id}/assess", damageId)
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(assessmentRequest)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.carStatusUpdated").exists());

                        Car updatedCar = carRepository.findById(car.getId()).orElseThrow();
                        assertThat(updatedCar.getCarStatusType()).isEqualTo(CarStatusType.MAINTENANCE);
                }

                @Test
                @DisplayName("Should handle damage with insurance coverage correctly")
                void shouldHandleDamageWithInsuranceCoverage() throws Exception {
                        User customer = TestDataBuilder.createTestUser();
                        customer = userRepository.save(customer);
                        String customerToken = generateUserToken(customer);

                        User admin = TestDataBuilder.createTestAdmin();
                        admin = userRepository.save(admin);
                        String adminToken = generateAdminToken(admin);

                        Car car = TestDataBuilder.createAvailableCar();
                        car = carRepository.save(car);

                        RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(car.getId());
                        Long rentalId = createAndGetRentalId(rentalRequest, customerToken);

                        mockMvc.perform(post("/api/v1/rentals/{id}/confirm", rentalId)
                                        .header("Authorization", "Bearer " + adminToken))
                                        .andExpect(status().isOk());

                        payRental(rentalId);
                        moveStartToToday(rentalId);
                        mockMvc.perform(post("/api/v1/rentals/{id}/pickup", rentalId)
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{}"))
                                        .andExpect(status().isOk());

                        mockMvc.perform(post("/api/v1/rentals/{id}/return", rentalId)
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{}"))
                                        .andExpect(status().isOk());

                        DamageReportRequest damageRequest = new DamageReportRequest(
                                        "Glass damage on windshield",
                                        "Front windshield",
                                        DamageSeverity.MODERATE,
                                        DamageCategory.GLASS_DAMAGE);

                        String damageResponse = mockMvc.perform(post("/api/v1/admin/damages")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .param("rentalId", rentalId.toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(damageRequest)))
                                        .andExpect(status().isCreated())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString();

                        Long damageId = objectMapper.readTree(damageResponse).get("id").asLong();

                        DamageAssessmentRequest assessmentRequest = new DamageAssessmentRequest(
                                        DamageSeverity.MODERATE,
                                        DamageCategory.GLASS_DAMAGE,
                                        new BigDecimal("3000.00"),
                                        true,
                                        new BigDecimal("500.00"),
                                        "Windshield replacement needed");

                        mockMvc.perform(post("/api/v1/admin/damages/{id}/assess", damageId)
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(assessmentRequest)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.damageId").value(damageId))
                                        .andExpect(jsonPath("$.repairCostEstimate").value(3000.00))
                                        .andExpect(jsonPath("$.insuranceCoverage").value(true))
                                        .andExpect(jsonPath("$.insuranceDeductible").value(500.00))
                                        .andExpect(jsonPath("$.customerLiability").value(500.00));
                }
        }

        @Nested
        @DisplayName("Authorization E2E Tests")
        class AuthorizationE2ETests {

                @Test
                @DisplayName("Should enforce admin-only access for damage report creation")
                void shouldEnforceAdminOnlyAccessForDamageReportCreation() throws Exception {
                        User customer = TestDataBuilder.createTestUser();
                        customer = userRepository.save(customer);
                        String customerToken = generateUserToken(customer);

                        User admin = TestDataBuilder.createTestAdmin();
                        admin = userRepository.save(admin);
                        String adminToken = generateAdminToken(admin);

                        Car car = TestDataBuilder.createAvailableCar();
                        car = carRepository.save(car);

                        RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(car.getId());
                        Long rentalId = createAndGetRentalId(rentalRequest, customerToken);

                        mockMvc.perform(post("/api/v1/rentals/{id}/confirm", rentalId)
                                        .header("Authorization", "Bearer " + adminToken))
                                        .andExpect(status().isOk());

                        DamageReportRequest damageRequest = new DamageReportRequest(
                                        "Test damage",
                                        "Test location",
                                        DamageSeverity.MINOR,
                                        DamageCategory.SCRATCH);

                        mockMvc.perform(post("/api/v1/admin/damages")
                                        .header("Authorization", "Bearer " + customerToken)
                                        .param("rentalId", rentalId.toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(damageRequest)))
                                        .andExpect(status().isForbidden());

                        mockMvc.perform(post("/api/v1/admin/damages")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .param("rentalId", rentalId.toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(damageRequest)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.code").value("DAMAGE_REPORT_ERROR"))
                                        .andExpect(jsonPath("$.detail").value(
                                                        org.hamcrest.Matchers.containsString("picked up")));

                        payRental(rentalId);
                        moveStartToToday(rentalId);
                        mockMvc.perform(post("/api/v1/rentals/{id}/pickup", rentalId)
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{}"))
                                        .andExpect(status().isOk());

                        mockMvc.perform(post("/api/v1/admin/damages")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .param("rentalId", rentalId.toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(damageRequest)))
                                        .andExpect(status().isCreated());
                }

                @Test
                @DisplayName("Should allow only rental owner to dispute damage")
                void shouldAllowOnlyRentalOwnerToDisputeDamage() throws Exception {
                        User customer = TestDataBuilder.createTestUser();
                        customer = userRepository.save(customer);
                        String customerToken = generateUserToken(customer);

                        User otherUser = TestDataBuilder.createTestUser("otheruser");
                        otherUser = userRepository.save(otherUser);
                        String otherUserToken = generateUserToken(otherUser);

                        User admin = TestDataBuilder.createTestAdmin();
                        admin = userRepository.save(admin);
                        String adminToken = generateAdminToken(admin);

                        Car car = TestDataBuilder.createAvailableCar();
                        car = carRepository.save(car);

                        RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(car.getId());
                        Long rentalId = createAndGetRentalId(rentalRequest, customerToken);

                        mockMvc.perform(post("/api/v1/rentals/{id}/confirm", rentalId)
                                        .header("Authorization", "Bearer " + adminToken))
                                        .andExpect(status().isOk());

                        payRental(rentalId);
                        moveStartToToday(rentalId);
                        mockMvc.perform(post("/api/v1/rentals/{id}/pickup", rentalId)
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{}"))
                                        .andExpect(status().isOk());

                        mockMvc.perform(post("/api/v1/rentals/{id}/return", rentalId)
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{}"))
                                        .andExpect(status().isOk());

                        DamageReportRequest damageRequest = new DamageReportRequest(
                                        "Test damage",
                                        "Test location",
                                        DamageSeverity.MINOR,
                                        DamageCategory.SCRATCH);

                        String damageResponse = mockMvc.perform(post("/api/v1/admin/damages")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .param("rentalId", rentalId.toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(damageRequest)))
                                        .andExpect(status().isCreated())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString();

                        Long damageId = objectMapper.readTree(damageResponse).get("id").asLong();

                        DamageAssessmentRequest assessmentRequest = new DamageAssessmentRequest(
                                        DamageSeverity.MINOR,
                                        DamageCategory.SCRATCH,
                                        new BigDecimal("300.00"),
                                        false,
                                        null,
                                        "Minor scratch repair");

                        mockMvc.perform(post("/api/v1/admin/damages/{id}/assess", damageId)
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(assessmentRequest)))
                                        .andExpect(status().isOk());

                        mockMvc.perform(post("/api/v1/admin/damages/{id}/charge", damageId)
                                        .header("Authorization", "Bearer " + adminToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.charged").value(true));

                        DamageDisputeRequest disputeRequest = new DamageDisputeRequest(
                                        "Dispute reason",
                                        "Comments");

                        mockMvc.perform(post("/api/v1/damages/{id}/dispute", damageId)
                                        .header("Authorization", "Bearer " + otherUserToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(disputeRequest)))
                                        .andExpect(status().isNotFound());

                        mockMvc.perform(post("/api/v1/damages/{id}/dispute", damageId)
                                        .header("Authorization", "Bearer " + customerToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(disputeRequest)))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.status").value("DISPUTED"));
                }
        }

        @Nested
        @DisplayName("Damage History and Statistics E2E Tests")
        class DamageHistoryE2ETests {

                @Test
                @DisplayName("Should track damage history for vehicle")
                void shouldTrackDamageHistoryForVehicle() throws Exception {
                        User customer = TestDataBuilder.createTestUser();
                        customer = userRepository.save(customer);
                        String customerToken = generateUserToken(customer);

                        User admin = TestDataBuilder.createTestAdmin();
                        admin = userRepository.save(admin);
                        String adminToken = generateAdminToken(admin);

                        Car car = TestDataBuilder.createAvailableCar();
                        car = carRepository.save(car);

                        RentalRequest rentalRequest1 = TestDataBuilder.createRentalRequest(car.getId());
                        Long rentalId1 = createAndGetRentalId(rentalRequest1, customerToken);

                        mockMvc.perform(post("/api/v1/rentals/{id}/confirm", rentalId1)
                                        .header("Authorization", "Bearer " + adminToken))
                                        .andExpect(status().isOk());

                        payRental(rentalId1);
                        moveStartToToday(rentalId1);
                        mockMvc.perform(post("/api/v1/rentals/{id}/pickup", rentalId1)
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{}"))
                                        .andExpect(status().isOk());

                        mockMvc.perform(post("/api/v1/rentals/{id}/return", rentalId1)
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{}"))
                                        .andExpect(status().isOk());

                        DamageReportRequest damageRequest = new DamageReportRequest(
                                        "First damage on this car",
                                        "Front bumper",
                                        DamageSeverity.MINOR,
                                        DamageCategory.SCRATCH);

                        mockMvc.perform(post("/api/v1/admin/damages")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .param("rentalId", rentalId1.toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(damageRequest)))
                                        .andExpect(status().isCreated());

                        mockMvc.perform(get("/api/v1/admin/damages/vehicle/{carId}", car.getId())
                                        .header("Authorization", "Bearer " + adminToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.content").isArray())
                                        .andExpect(jsonPath("$.content.length()").value(1))
                                        .andExpect(jsonPath("$.content[0].description")
                                                        .value("First damage on this car"));
                }

                @Test
                @DisplayName("Should allow customer to view their own damage history")
                void shouldAllowCustomerToViewOwnDamageHistory() throws Exception {
                        User customer = TestDataBuilder.createTestUser();
                        customer = userRepository.save(customer);
                        String customerToken = generateUserToken(customer);

                        User admin = TestDataBuilder.createTestAdmin();
                        admin = userRepository.save(admin);
                        String adminToken = generateAdminToken(admin);

                        Car car = TestDataBuilder.createAvailableCar();
                        car = carRepository.save(car);

                        RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(car.getId());
                        Long rentalId = createAndGetRentalId(rentalRequest, customerToken);

                        mockMvc.perform(post("/api/v1/rentals/{id}/confirm", rentalId)
                                        .header("Authorization", "Bearer " + adminToken))
                                        .andExpect(status().isOk());

                        payRental(rentalId);
                        moveStartToToday(rentalId);
                        mockMvc.perform(post("/api/v1/rentals/{id}/pickup", rentalId)
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{}"))
                                        .andExpect(status().isOk());

                        mockMvc.perform(post("/api/v1/rentals/{id}/return", rentalId)
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{}"))
                                        .andExpect(status().isOk());

                        DamageReportRequest damageRequest = new DamageReportRequest(
                                        "Customer's damage",
                                        "Rear bumper",
                                        DamageSeverity.MINOR,
                                        DamageCategory.SCRATCH);

                        mockMvc.perform(post("/api/v1/admin/damages")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .param("rentalId", rentalId.toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(damageRequest)))
                                        .andExpect(status().isCreated());

                        mockMvc.perform(get("/api/v1/damages/me")
                                        .header("Authorization", "Bearer " + customerToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.content").isArray())
                                        .andExpect(jsonPath("$.content.length()").value(1))
                                        .andExpect(jsonPath("$.content[0].description").value("Customer's damage"));
                }
        }
}
