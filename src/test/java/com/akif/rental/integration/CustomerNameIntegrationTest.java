package com.akif.rental.integration;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.api.CarStatusType;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.damage.api.DamageCategory;
import com.akif.damage.api.DamageSeverity;
import com.akif.damage.api.DamageStatus;
import com.akif.damage.domain.model.DamageReport;
import com.akif.damage.internal.repository.DamageReportRepository;
import com.akif.rental.api.RentalRequest;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.Role;
import com.akif.shared.security.JwtTokenService;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Denormalised customer name")
class CustomerNameIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private com.akif.rental.internal.repository.RentalRepository rentalRepository;

    @Autowired
    private DamageReportRepository damageReportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService tokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User customer;
    private Car car;
    private String token;

    @BeforeEach
    void setUp() {
        String id = randomUUID().toString().substring(0, 6);
        customer = userRepository.save(User.builder()
                .username("nameless_" + id)
                .email("nameless_" + id + "@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles(new java.util.HashSet<>(Set.of(Role.USER)))
                .enabled(true)
                .build());
        car = carRepository.save(Car.builder()
                .licensePlate("34NM" + id)
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2020)
                .price(new BigDecimal("500.00"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .build());
        Authentication auth = new UsernamePasswordAuthenticationToken(
                customer.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        token = "Bearer " + tokenProvider.generateAccessToken(auth, customer.getId());
    }

    private long book() throws Exception {
        RentalRequest request = new RentalRequest(
                car.getId(), LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), null);
        String body = mockMvc.perform(post("/api/v1/rentals/request")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private String storedRentalName(long rentalId) {
        return jdbcTemplate.queryForObject(
                "SELECT user_full_name FROM car_rental.rentals WHERE id = ?", String.class, rentalId);
    }

    @Test
    @DisplayName("a customer without a name is booked under the username, never as null null")
    void namelessCustomerFallsBackToUsername() throws Exception {
        long rentalId = book();

        assertThat(storedRentalName(rentalId)).isEqualTo(customer.getUsername());
    }

    @Test
    @DisplayName("the name on rentals and damage reports follows the profile when the customer fills it in")
    void profileUpdateRefreshesStoredNames() throws Exception {
        long rentalId = book();
        DamageReport damage = damageReportRepository.save(DamageReport.builder()
                .rentalId(rentalId)
                .carId(car.getId())
                .carBrand(car.getBrand())
                .carModel(car.getModel())
                .carLicensePlate(car.getLicensePlate())
                .rentalStartDate(LocalDate.now().plusDays(1))
                .rentalEndDate(LocalDate.now().plusDays(3))
                .customerEmail(customer.getEmail())
                .customerFullName(customer.getUsername())
                .customerUserId(customer.getId())
                .description("Scratch")
                .damageLocation("Door")
                .severity(DamageSeverity.MINOR)
                .category(DamageCategory.SCRATCH)
                .status(DamageStatus.REPORTED)
                .reportedBy(customer.getId())
                .reportedAt(Instant.now())
                .build());

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"phone\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ada"));

        assertThat(storedRentalName(rentalId)).isEqualTo("Ada Lovelace");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT customer_full_name FROM car_rental.damage_reports WHERE id = ?",
                String.class, damage.getId())).isEqualTo("Ada Lovelace");
    }

    @Test
    @DisplayName("the repair migration leaves names that only contain the letters null inside a word")
    void repairMigrationKeepsWordsContainingNull() throws Exception {
        long rentalId = book();
        jdbcTemplate.update("UPDATE car_rental.rentals SET user_full_name = ? WHERE id = ?", "Nullman Annull", rentalId);

        String script = new String(new org.springframework.core.io.ClassPathResource(
                "db/migration/V25__repair_null_customer_names.sql").getInputStream().readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        jdbcTemplate.execute(script);

        assertThat(storedRentalName(rentalId)).isEqualTo("Nullman Annull");
    }

    @Test
    @DisplayName("the bulk name refresh bumps the version and update time so stale writers are rejected")
    void bulkRefreshBumpsVersionAndUpdateTime() throws Exception {
        long rentalId = book();
        Long versionBefore = jdbcTemplate.queryForObject(
                "SELECT version FROM car_rental.rentals WHERE id = ?", Long.class, rentalId);
        java.sql.Timestamp updatedBefore = jdbcTemplate.queryForObject(
                "SELECT update_time FROM car_rental.rentals WHERE id = ?", java.sql.Timestamp.class, rentalId);

        rentalRepository.refreshCustomerName(customer.getId(), "Grace Hopper", java.time.Instant.now().plusSeconds(60));

        assertThat(storedRentalName(rentalId)).isEqualTo("Grace Hopper");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT version FROM car_rental.rentals WHERE id = ?", Long.class, rentalId)).isEqualTo(versionBefore + 1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT update_time FROM car_rental.rentals WHERE id = ?", java.sql.Timestamp.class, rentalId))
                .isAfterOrEqualTo(updatedBefore);
    }

    @Test
    @DisplayName("the repair migration replaces null null with the current name or the username")
    void repairMigrationReplacesNullNames() throws Exception {
        long rentalId = book();
        jdbcTemplate.update("UPDATE car_rental.rentals SET user_full_name = 'null null' WHERE id = ?", rentalId);

        String script = new String(new org.springframework.core.io.ClassPathResource(
                "db/migration/V25__repair_null_customer_names.sql").getInputStream().readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        jdbcTemplate.execute(script);

        assertThat(storedRentalName(rentalId)).isEqualTo(customer.getUsername());
    }
}
