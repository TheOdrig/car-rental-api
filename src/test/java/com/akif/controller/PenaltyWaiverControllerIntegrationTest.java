package com.akif.controller;

import com.akif.dto.request.PenaltyWaiverRequestDto;
import com.akif.enums.CarStatusType;
import com.akif.enums.CurrencyType;
import com.akif.enums.LateReturnStatus;
import com.akif.enums.RentalStatus;
import com.akif.enums.Role;
import com.akif.model.Car;
import com.akif.model.PenaltyWaiver;
import com.akif.model.Rental;
import com.akif.model.User;
import com.akif.repository.CarRepository;
import com.akif.repository.PenaltyWaiverRepository;
import com.akif.repository.RentalRepository;
import com.akif.repository.UserRepository;
import com.akif.scheduler.LateReturnScheduler;
import com.akif.service.email.IEmailSender;
import com.akif.starter.CarGalleryProjectApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("PenaltyWaiverController Integration Tests")
public class PenaltyWaiverControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PenaltyWaiverRepository penaltyWaiverRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IEmailSender emailSender;

    @MockitoBean
    private LateReturnScheduler lateReturnScheduler;

    private User testUser;
    private User adminUser;
    private Car testCar;
    private Rental lateRental;

    @BeforeEach
    void setUp() {
        penaltyWaiverRepository.deleteAll();
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

        adminUser = User.builder()
                .username("admin")
                .email("admin@example.com")
                .password("password")
                .roles(Set.of(Role.ADMIN))
                .enabled(true)
                .isDeleted(false)
                .build();
        adminUser = userRepository.save(adminUser);

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
                .status(RentalStatus.RETURNED)
                .lateReturnStatus(LateReturnStatus.LATE)
                .lateDetectedAt(LocalDateTime.now().minusDays(1))
                .lateHours(30)
                .penaltyAmount(new BigDecimal("1500"))
                .penaltyPaid(false)
                .build();
        lateRental = rentalRepository.save(lateRental);
    }

    @AfterEach
    void tearDown() {
        penaltyWaiverRepository.deleteAll();
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("POST /api/admin/rentals/{id}/penalty/waive - Should waive full penalty")
    void shouldWaiveFullPenalty() throws Exception {
        PenaltyWaiverRequestDto request = PenaltyWaiverRequestDto.builder()
                .fullWaiver(true)
                .reason("Customer complaint - exceptional circumstances")
                .build();

        mockMvc.perform(post("/api/admin/rentals/{id}/penalty/waive", lateRental.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.rentalId").value(lateRental.getId()))
                .andExpect(jsonPath("$.originalPenalty").value(1500))
                .andExpect(jsonPath("$.waivedAmount").value(1500))
                .andExpect(jsonPath("$.remainingPenalty").value(0))
                .andExpect(jsonPath("$.reason").value("Customer complaint - exceptional circumstances"))
                .andExpect(jsonPath("$.adminId").value(adminUser.getId()))
                .andExpect(jsonPath("$.waivedAt").exists());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("POST /api/admin/rentals/{id}/penalty/waive - Should waive partial penalty")
    void shouldWaivePartialPenalty() throws Exception {
        PenaltyWaiverRequestDto request = PenaltyWaiverRequestDto.builder()
                .fullWaiver(false)
                .waiverAmount(new BigDecimal("500"))
                .reason("Partial waiver due to system error during rental period")
                .build();

        mockMvc.perform(post("/api/admin/rentals/{id}/penalty/waive", lateRental.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.rentalId").value(lateRental.getId()))
                .andExpect(jsonPath("$.originalPenalty").value(1500))
                .andExpect(jsonPath("$.waivedAmount").value(500))
                .andExpect(jsonPath("$.remainingPenalty").value(1000))
                .andExpect(jsonPath("$.reason").value("Partial waiver due to system error during rental period"))
                .andExpect(jsonPath("$.adminId").value(adminUser.getId()));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("POST /api/admin/rentals/{id}/penalty/waive - Should reject invalid waiver amount")
    void shouldRejectInvalidWaiverAmount() throws Exception {
        PenaltyWaiverRequestDto request = PenaltyWaiverRequestDto.builder()
                .fullWaiver(false)
                .waiverAmount(new BigDecimal("-100"))
                .reason("Invalid negative amount test")
                .build();

        mockMvc.perform(post("/api/admin/rentals/{id}/penalty/waive", lateRental.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("POST /api/admin/rentals/{id}/penalty/waive - Should reject short reason")
    void shouldRejectShortReason() throws Exception {
        PenaltyWaiverRequestDto request = PenaltyWaiverRequestDto.builder()
                .fullWaiver(true)
                .reason("Short")
                .build();

        mockMvc.perform(post("/api/admin/rentals/{id}/penalty/waive", lateRental.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("GET /api/admin/rentals/{id}/penalty/history - Should return penalty history")
    void shouldReturnPenaltyHistory() throws Exception {
        PenaltyWaiver waiver = PenaltyWaiver.builder()
                .rental(lateRental)
                .originalPenalty(new BigDecimal("1500"))
                .waivedAmount(new BigDecimal("500"))
                .remainingPenalty(new BigDecimal("1000"))
                .reason("First waiver - partial")
                .adminId(adminUser.getId())
                .waivedAt(LocalDateTime.now())
                .refundInitiated(false)
                .build();
        penaltyWaiverRepository.save(waiver);

        mockMvc.perform(get("/api/admin/rentals/{id}/penalty/history", lateRental.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].rentalId").value(lateRental.getId()))
                .andExpect(jsonPath("$[0].originalPenalty").value(1500))
                .andExpect(jsonPath("$[0].waivedAmount").value(500))
                .andExpect(jsonPath("$[0].remainingPenalty").value(1000))
                .andExpect(jsonPath("$[0].reason").value("First waiver - partial"))
                .andExpect(jsonPath("$[0].adminId").value(adminUser.getId()));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("GET /api/admin/rentals/{id}/penalty/history - Should return empty list for no waivers")
    void shouldReturnEmptyListForNoWaivers() throws Exception {
        mockMvc.perform(get("/api/admin/rentals/{id}/penalty/history", lateRental.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("GET /api/admin/rentals/{id}/penalty/history - Should return multiple waivers")
    void shouldReturnMultipleWaivers() throws Exception {
        PenaltyWaiver waiver1 = PenaltyWaiver.builder()
                .rental(lateRental)
                .originalPenalty(new BigDecimal("1500"))
                .waivedAmount(new BigDecimal("500"))
                .remainingPenalty(new BigDecimal("1000"))
                .reason("First waiver")
                .adminId(adminUser.getId())
                .waivedAt(LocalDateTime.now().minusDays(1))
                .refundInitiated(false)
                .build();
        penaltyWaiverRepository.save(waiver1);

        PenaltyWaiver waiver2 = PenaltyWaiver.builder()
                .rental(lateRental)
                .originalPenalty(new BigDecimal("1000"))
                .waivedAmount(new BigDecimal("500"))
                .remainingPenalty(new BigDecimal("500"))
                .reason("Second waiver")
                .adminId(adminUser.getId())
                .waivedAt(LocalDateTime.now())
                .refundInitiated(false)
                .build();
        penaltyWaiverRepository.save(waiver2);

        mockMvc.perform(get("/api/admin/rentals/{id}/penalty/history", lateRental.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    @DisplayName("POST /api/admin/rentals/{id}/penalty/waive - Should deny access for non-admin")
    void shouldDenyAccessForNonAdminWaive() throws Exception {
        PenaltyWaiverRequestDto request = PenaltyWaiverRequestDto.builder()
                .fullWaiver(true)
                .reason("Unauthorized attempt to waive penalty")
                .build();

        mockMvc.perform(post("/api/admin/rentals/{id}/penalty/waive", lateRental.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/admin/rentals/{id}/penalty/waive - Should deny access for unauthenticated")
    void shouldDenyAccessForUnauthenticatedWaive() throws Exception {
        PenaltyWaiverRequestDto request = PenaltyWaiverRequestDto.builder()
                .fullWaiver(true)
                .reason("Unauthenticated attempt to waive penalty")
                .build();

        mockMvc.perform(post("/api/admin/rentals/{id}/penalty/waive", lateRental.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    @DisplayName("GET /api/admin/rentals/{id}/penalty/history - Should deny access for non-admin")
    void shouldDenyAccessForNonAdminHistory() throws Exception {
        mockMvc.perform(get("/api/admin/rentals/{id}/penalty/history", lateRental.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/rentals/{id}/penalty/history - Should deny access for unauthenticated")
    void shouldDenyAccessForUnauthenticatedHistory() throws Exception {
        mockMvc.perform(get("/api/admin/rentals/{id}/penalty/history", lateRental.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("POST /api/admin/rentals/{id}/penalty/waive - Should return 404 for non-existent rental")
    void shouldReturn404ForNonExistentRental() throws Exception {
        PenaltyWaiverRequestDto request = PenaltyWaiverRequestDto.builder()
                .fullWaiver(true)
                .reason("Testing non-existent rental waiver")
                .build();

        mockMvc.perform(post("/api/admin/rentals/{id}/penalty/waive", 99999L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
