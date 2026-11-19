package com.akif.rental.integration;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.exception.PenaltyWaiverException;
import com.akif.rental.internal.repository.PenaltyWaiverRepository;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.rental.internal.scheduler.ReminderScheduler;
import com.akif.rental.internal.service.penalty.PenaltyWaiverService;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Penalty waivers under concurrency")
class PenaltyWaiverConcurrencyTest {

    @Autowired
    private PenaltyWaiverService waiverService;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PenaltyWaiverRepository waiverRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private ReminderScheduler reminderScheduler;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM car_rental.penalty_waivers");
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("a waiver waits for a concurrent change to the rental and then sees the reduced penalty")
    void waiverSeesTheCommittedPenalty() throws Exception {
        User customer = userRepository.save(TestDataBuilder.createTestUser());
        Car car = carRepository.save(TestDataBuilder.createAvailableCar());
        Rental rental = TestDataBuilder.createRental(car, customer, LocalDate.now().minusDays(5),
                LocalDate.now().minusDays(2), RentalStatus.RETURNED);
        rental.assessPenalty(new BigDecimal("100.00"), 10);
        Long rentalId = rentalRepository.saveAndFlush(rental).getId();

        CountDownLatch locked = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<?> concurrentWaiver = pool.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        Rental held = rentalRepository.findByIdForUpdate(rentalId).orElseThrow();
                        held.reducePenaltyTo(new BigDecimal("40.00"));
                        rentalRepository.saveAndFlush(held);
                        locked.countDown();
                        sleep(800);
                    }));
            assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> waiverService.waivePenalty(rentalId, new BigDecimal("60.00"), "goodwill", 1L))
                    .isInstanceOf(PenaltyWaiverException.class)
                    .hasMessageContaining("cannot exceed the outstanding penalty");

            concurrentWaiver.get(10, TimeUnit.SECONDS);
            assertThat(rentalRepository.findById(rentalId).orElseThrow().getPenaltyAmount()).isEqualByComparingTo("40.00");
            assertThat(waiverRepository.findByRentalIdAndIsDeletedFalse(rentalId)).isEmpty();
        } finally {
            pool.shutdownNow();
        }
    }

    private static void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
