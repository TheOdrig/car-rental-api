package com.akif.payment.e2e;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.payment.domain.Payment;
import com.akif.payment.domain.enums.PaymentKind;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.shared.enums.CurrencyType;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@DisplayName("Zero-decimal currency rental flow E2E Tests")
class JpyRentalFlowE2ETest extends E2ETestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Test
    @DisplayName("a JPY rental is priced in whole yen, confirms, and its payment intent succeeds")
    void jpyRentalCanBePaid() throws Exception {
        User customer = userRepository.save(TestDataBuilder.createTestUser());
        User admin = userRepository.save(TestDataBuilder.createTestAdmin());
        String customerToken = generateUserToken(customer);
        String adminToken = generateAdminToken(admin);
        Car car = TestDataBuilder.createAvailableCar("34JPY001", new BigDecimal("12345.67"));
        car.setCurrencyType(CurrencyType.JPY);
        car = carRepository.save(car);

        Long rentalId = createAndGetRentalId(TestDataBuilder.createRentalRequest(car.getId()), customerToken);
        Rental rental = rentalRepository.findById(rentalId).orElseThrow();
        assertThat(rental.getCurrency()).isEqualTo(CurrencyType.JPY);
        assertThat(rental.getTotalPrice().stripTrailingZeros().scale()).isLessThanOrEqualTo(0);
        assertThat(rental.getTotalPrice().scale()).isZero();
        assertThat(rental.getDailyPrice().scale()).isZero();

        mockMvc.perform(post("/api/v1/rentals/{id}/confirm", rentalId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/rentals/{id}/payment-intent", rentalId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rentalId").value(rentalId))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));

        Payment payment = paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rentalId, PaymentKind.RENTAL)
                .orElseThrow();
        assertThat(payment.getCurrency()).isEqualTo(CurrencyType.JPY);
        assertThat(payment.getAmount()).isEqualByComparingTo(rental.getTotalPrice());
    }
}
