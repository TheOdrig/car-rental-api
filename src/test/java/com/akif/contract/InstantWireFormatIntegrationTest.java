package com.akif.contract;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.api.CarStatusType;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.enums.LateReturnStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.Role;
import com.akif.shared.security.JwtTokenService;
import com.akif.testsupport.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Instants cross the wire as ISO-8601 UTC")
class InstantWireFormatIntegrationTest {

    private static final String UTC_INSTANT = "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}([.][0-9]{1,9})?Z";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private JwtTokenService tokenService;

    private User customer;
    private User admin;
    private Rental rental;

    @BeforeEach
    void setUp() {
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();

        customer = userRepository.save(User.builder()
                .username("wirecustomer")
                .email("wire.customer@example.com")
                .password("x")
                .firstName("Wire")
                .lastName("Customer")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build());
        admin = userRepository.save(User.builder()
                .username("wireadmin")
                .email("wire.admin@example.com")
                .password("x")
                .firstName("Wire")
                .lastName("Admin")
                .roles(Set.of(Role.USER, Role.ADMIN))
                .enabled(true)
                .build());
        Car car = carRepository.save(Car.builder()
                .licensePlate("34WIRE01")
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2020)
                .price(new BigDecimal("500.00"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .build());
        rental = rentalRepository.save(Rental.builder()
                .userId(customer.getId())
                .carId(car.getId())
                .carBrand(car.getBrand())
                .carModel(car.getModel())
                .carLicensePlate(car.getLicensePlate())
                .userEmail(customer.getEmail())
                .userFullName("Wire Customer")
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(8))
                .days(3)
                .currency(CurrencyType.TRY)
                .dailyPrice(new BigDecimal("500.00"))
                .totalPrice(new BigDecimal("1500.00"))
                .status(RentalStatus.REQUESTED)
                .lateReturnStatus(LateReturnStatus.ON_TIME)
                .build());
    }

    @AfterEach
    void tearDown() {
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String bearer(User user) {
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
        return "Bearer " + tokenService.generateAccessToken(authentication, user.getId());
    }

    @Test
    @DisplayName("an admin user detail carries a Z-suffixed registration instant that round-trips")
    void adminUserDetailTimestamp() throws Exception {
        String body = mockMvc.perform(get("/api/v1/admin/users/{id}", customer.getId())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String registered = JsonPath.read(body, "$.registrationDate");

        assertThat(registered).matches(UTC_INSTANT);
        Instant stored = userRepository.findById(customer.getId()).orElseThrow().getCreateTime();
        assertThat(Instant.parse(registered)).isEqualTo(stored);
        assertThat(Instant.parse(registered).toString()).isEqualTo(registered);
    }

    @Test
    @DisplayName("a rental carries Z-suffixed create and update instants that round-trip")
    void rentalTimestamps() throws Exception {
        String body = mockMvc.perform(get("/api/v1/rentals/{id}", rental.getId())
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String created = JsonPath.read(body, "$.createTime");
        String updated = JsonPath.read(body, "$.updateTime");

        assertThat(created).matches(UTC_INSTANT);
        assertThat(updated).matches(UTC_INSTANT);
        Rental stored = rentalRepository.findById(rental.getId()).orElseThrow();
        assertThat(Instant.parse(created)).isEqualTo(stored.getCreateTime());
        assertThat(Instant.parse(updated)).isEqualTo(stored.getUpdateTime());
        assertThat(JsonPath.<String>read(body, "$.status")).isEqualTo("REQUESTED");
        assertThat(JsonPath.<String>read(body, "$.startDate")).isEqualTo(rental.getStartDate().toString());
    }
}
