package com.akif.rental.integration;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.api.CarStatusType;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.Payment;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.enums.LateReturnStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.Role;
import com.akif.shared.security.JwtTokenService;
import com.akif.testsupport.TestcontainersConfiguration;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class, properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("My rentals statement counts")
class MyRentalsQueryCountIntegrationTest {

    private static final long MAX_STATEMENTS = 6;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JwtTokenService tokenService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private User customer;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
        customer = userRepository.save(User.builder()
                .username("countcustomer")
                .email("count.customer@example.com")
                .password("x")
                .firstName("Count")
                .lastName("Customer")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build());
        for (int index = 0; index < 12; index++) {
            Car car = carRepository.save(Car.builder()
                    .licensePlate(String.format("34CNT%03d", index))
                    .brand("Toyota")
                    .model("Model" + index)
                    .productionYear(2020)
                    .price(new BigDecimal("500.00"))
                    .currencyType(CurrencyType.TRY)
                    .carStatusType(CarStatusType.AVAILABLE)
                    .build());
            Rental rental = rentalRepository.save(Rental.builder()
                    .userId(customer.getId())
                    .carId(car.getId())
                    .carBrand(car.getBrand())
                    .carModel(car.getModel())
                    .carLicensePlate(car.getLicensePlate())
                    .userEmail(customer.getEmail())
                    .userFullName("Count Customer")
                    .startDate(LocalDate.now().plusDays(5 + index))
                    .endDate(LocalDate.now().plusDays(7 + index))
                    .days(2)
                    .currency(CurrencyType.TRY)
                    .dailyPrice(new BigDecimal("500.00"))
                    .totalPrice(new BigDecimal("1000.00"))
                    .status(RentalStatus.REQUESTED)
                    .lateReturnStatus(LateReturnStatus.ON_TIME)
                    .build());
            if (index % 2 == 0) {
                paymentRepository.save(Payment.builder()
                        .rentalId(rental.getId())
                        .amount(new BigDecimal("1000.00"))
                        .currency(CurrencyType.TRY)
                        .status(PaymentStatus.AUTHORIZED)
                        .build());
            }
        }
    }

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private long statementsFor(int size) throws Exception {
        String token = tokenService.generateAccessToken(new UsernamePasswordAuthenticationToken(
                customer.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))), customer.getId());
        statistics().clear();
        mockMvc.perform(get("/api/v1/rentals/me").param("size", String.valueOf(size))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(size))
                .andExpect(jsonPath("$.content[?(@.paymentStatus == 'AUTHORIZED')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(!@.paymentStatus)]").isNotEmpty());
        return statistics().getPrepareStatementCount();
    }

    @Test
    @DisplayName("the caller's rentals load with a bounded number of statements regardless of page size")
    void statementCountIsIndependentOfPageSize() throws Exception {
        long small = statementsFor(2);
        long large = statementsFor(12);

        assertThat(small).isLessThanOrEqualTo(MAX_STATEMENTS);
        assertThat(large).isEqualTo(small);
    }
}
