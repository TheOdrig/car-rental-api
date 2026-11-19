package com.akif.rental.e2e.rental;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.payment.api.CreatePaymentRequest;
import com.akif.payment.api.PaymentDto;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.Payment;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import com.akif.rental.api.RentalCancelledEvent;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.scheduler.NoShowRentalScheduler;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@DisplayName("No-show release E2E Tests")
class NoShowReleaseE2ETest extends E2ETestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private NoShowRentalScheduler noShowScheduler;

    @MockitoSpyBean
    private PaymentGateway paymentGateway;

    private User customer;
    private Car car;

    private Rental confirmedRentalWithHold(int startedDaysAgo) {
        customer = userRepository.save(TestDataBuilder.createTestUser());
        car = carRepository.save(TestDataBuilder.createAvailableCar());
        LocalDate today = LocalDate.now(clock);
        Rental rental = rentalRepository.save(TestDataBuilder.createRental(
                car, customer, today.minusDays(startedDaysAgo), today.plusDays(3), RentalStatus.CONFIRMED));
        PaymentDto dto = paymentService.createPayment(new CreatePaymentRequest(
                rental.getId(), rental.getUserEmail(), rental.getCarLicensePlate(),
                rental.getTotalPrice(), rental.getCurrency(), "CARD"));
        paymentService.createOrGetIntentForRental(rental.getId());
        assertThat(paymentRepository.findById(dto.id()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        return rental;
    }

    private Payment paymentOf(Rental rental) {
        return paymentRepository.findAll().stream()
                .filter(payment -> payment.getRentalId().equals(rental.getId()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("a confirmed rental that was never picked up releases its hold and is cancelled")
    void noShowReleasesTheHold() {
        Rental rental = confirmedRentalWithHold(1);
        String intentId = paymentOf(rental).getStripePaymentIntentId();

        int released = noShowScheduler.releaseNoShows();

        assertThat(released).isEqualTo(1);
        assertThat(rentalRepository.findById(rental.getId()).orElseThrow().getStatus()).isEqualTo(RentalStatus.CANCELLED);
        Payment payment = paymentOf(rental);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(payment.isClosed()).isTrue();
        verify(paymentGateway).cancel(eq(intentId), anyString());
        List<RentalCancelledEvent> events = eventCaptor.getEventsOfType(RentalCancelledEvent.class);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).cancellationReason()).contains("not picked up");
        assertThat(events.get(0).refundProcessed()).isTrue();
    }

    @Test
    @DisplayName("a rental that starts today is still waiting for its customer and keeps its hold")
    void rentalStartingTodayIsLeftAlone() {
        Rental rental = confirmedRentalWithHold(0);

        int released = noShowScheduler.releaseNoShows();

        assertThat(released).isZero();
        assertThat(rentalRepository.findById(rental.getId()).orElseThrow().getStatus()).isEqualTo(RentalStatus.CONFIRMED);
        assertThat(paymentOf(rental).getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        verify(paymentGateway, never()).cancel(anyString(), anyString());
    }

    @Test
    @DisplayName("the sweep is idempotent")
    void sweepingTwiceReleasesOnce() {
        confirmedRentalWithHold(2);

        assertThat(noShowScheduler.releaseNoShows()).isEqualTo(1);
        assertThat(noShowScheduler.releaseNoShows()).isZero();
    }

    @Test
    @DisplayName("pickup is refused after the end date")
    void pickupAfterTheEndDateIsRefused() throws Exception {
        customer = userRepository.save(TestDataBuilder.createTestUser());
        User admin = userRepository.save(TestDataBuilder.createTestAdmin());
        car = carRepository.save(TestDataBuilder.createAvailableCar());
        LocalDate today = LocalDate.now(clock);
        Rental rental = rentalRepository.save(TestDataBuilder.createRental(
                car, customer, today.minusDays(5), today.minusDays(2), RentalStatus.CONFIRMED));
        paymentService.createPayment(new CreatePaymentRequest(
                rental.getId(), rental.getUserEmail(), rental.getCarLicensePlate(),
                rental.getTotalPrice(), rental.getCurrency(), "CARD"));
        paymentService.createOrGetIntentForRental(rental.getId());

        mockMvc.perform(post("/api/v1/rentals/{id}/pickup", rental.getId())
                        .header("Authorization", "Bearer " + generateAdminToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RENTAL_VALIDATION_FAILED"));

        assertThat(rentalRepository.findById(rental.getId()).orElseThrow().getStatus()).isEqualTo(RentalStatus.CONFIRMED);
        assertThat(paymentOf(rental).getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    }
}
